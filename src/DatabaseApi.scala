package usql

import java.sql.{ResultSet, Statement}
import Types.Id
import upickle.default.ReadWriter
trait QueryRunnable[T, V]{
  def rw: ReadWriter[V]
  def primitive: Boolean
}

object QueryRunnable{
  implicit def containerQr[E[_] <: Expr[_], V[_[_]]](implicit rw0: ReadWriter[V[Id]]): QueryRunnable[V[E], V[Id]] = {
    new QueryRunnable[V[E], V[Id]] {
      def rw = rw0
      def primitive = false
    }
  }

  implicit def exprQr[E[_] <: Expr[_], T](implicit rw0: ReadWriter[T]): QueryRunnable[E[T], T] = {
    new QueryRunnable[E[T], T] {
      def rw = rw0
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
    val resultSet: ResultSet = statement.executeQuery(query.toSqlQuery)

    val res = collection.mutable.Buffer.empty[V]
    try {
      while (resultSet.next()) {
        if (qr.primitive){
          res.append(upickle.default.read[V](ujson.Str(resultSet.getString(1)))(qr.rw))
        } else{
          val obj = ujson.Obj()
          val meta = resultSet.getMetaData
          for (i <- Range(0, meta.getColumnCount)) {
            obj(meta.getColumnName(i + 1).toLowerCase) = resultSet.getString(i + 1)
          }

          res.append(upickle.default.read[V](obj)(qr.rw))
        }

      }
    } finally {
      resultSet.close()
      statement.close()
    }
    res
  }
}
