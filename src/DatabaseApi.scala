package usql

import java.sql.{ResultSet, Statement}
import upickle.default.{Reader, Writer}

trait QueryRunnable[T, V]{
  def valueReader: Reader[V]
  def queryWriter: Writer[T]
  def primitive: Boolean
}

object QueryRunnable{
  implicit def containerQr[E[_] <: Expr[_], V[_[_]]](implicit valueReader0: Reader[V[Val]],
                                                     queryWriter0: Writer[V[E]]): QueryRunnable[V[E], V[Val]] = {
    new QueryRunnable[V[E], V[Val]] {
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

  def run[T, V](query: Query[T])
               (implicit qr: QueryRunnable[T, V]) = {

    val statement: Statement = connection.createStatement()

    val exprStr = FlatJson.flatten(upickle.default.writeJs(query.expr)(qr.queryWriter))
      .map{case (k, v) => s"""$v as $k"""}
      .mkString(", ")

    val queryStr = query.toSqlQuery(exprStr)
    val resultSet: ResultSet = statement.executeQuery(queryStr)

    val res = collection.mutable.Buffer.empty[V]
    try {
      while (resultSet.next()) {
        val kvs = collection.mutable.Buffer.empty[(String, String)]
        val meta = resultSet.getMetaData

        for (i <- Range(0, meta.getColumnCount)) {
          kvs.append((meta.getColumnLabel(i + 1).toLowerCase, resultSet.getString(i + 1)))
        }

        val json = FlatJson.unflatten(kvs.toSeq)

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
