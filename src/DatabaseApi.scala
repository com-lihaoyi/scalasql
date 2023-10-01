package usql

import java.sql.{ResultSet, Statement}
import Types.Id
import upickle.default.{Reader, Writer}

trait QueryRunnable[T, V]{
  def valueReader: Reader[V]
  def queryWriter: Writer[T]
  def primitive: Boolean
}

object QueryRunnable{
  implicit def containerQr[E[_] <: Expr[_], V[_[_]]](implicit valueReader0: Reader[V[Id]],
                                                     queryWriter0: Writer[V[E]]): QueryRunnable[V[E], V[Id]] = {
    new QueryRunnable[V[E], V[Id]] {
      def valueReader = valueReader0
      def queryWriter = queryWriter0
      def primitive = false
    }
  }

  implicit def exprQr[E[_] <: Expr[_], T](implicit valueReader0: Reader[T],
                                          queryWriter0: Writer[E[T]]): QueryRunnable[E[T], T] = {
    new QueryRunnable[E[T], T] {
      def valueReader = valueReader0
      def queryWriter = queryWriter0
      def primitive = true
    }
  }
}

class DatabaseApi(connection: java.sql.Connection) {
  def runRaw(sql: String) = {
    val statement: Statement = connection.createStatement()
    try statement.execute(sql)
    finally statement.close()
  }

  def flattenJson(x: ujson.Value,
                  prefix: String): Seq[(String, String)] = {
    x match{
      case ujson.Obj(kvs) =>
        kvs.toSeq.flatMap{case (k, v) => flattenJson(v, prefix + "__" + k) }
      case ujson.Str(s) => Seq(prefix -> s)
    }
  }
  def unflattenJson(kvs: Seq[(String, String)]): ujson.Value = {
    val root = ujson.Obj()

    for((k, v) <- kvs){
      val segments = k.split("__")
      var current = root
      for(s <- segments.init){
        if (!current.value.contains(s)) current(s) = ujson.Obj()
        current = current(s).asInstanceOf[ujson.Obj]
      }

      current(segments.last) = v
    }

    root
  }

  def run[T, V](query: Query[T])
               (implicit qr: QueryRunnable[T, V]) = {

    val statement: Statement = connection.createStatement()

    val exprStr = flattenJson(upickle.default.writeJs(query.expr)(qr.queryWriter), "res")
      .map{case (k, v) => s"""$v as $k"""}
      .mkString(", ")

    val queryStr = query.toSqlQuery(exprStr)
    pprint.log(queryStr)
    val resultSet: ResultSet = statement.executeQuery(queryStr)

    val res = collection.mutable.Buffer.empty[V]

    try {
      while (resultSet.next()) {
        val kvs = collection.mutable.Buffer.empty[(String, String)]
        val meta = resultSet.getMetaData

        for (i <- Range(0, meta.getColumnCount)) {
          kvs.append((meta.getColumnLabel(i + 1).toLowerCase, resultSet.getString(i + 1)))
        }

        val json = unflattenJson(kvs.toSeq)("res")

        pprint.log(json.render(4))
        res.append(upickle.default.read[V](json)(qr.valueReader))
      }
    } finally {
      resultSet.close()
      statement.close()
    }
    res
  }
}
