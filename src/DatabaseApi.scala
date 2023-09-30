package usql

import java.sql.{ResultSet, Statement}
import Types.Id

class DatabaseApi(connection: java.sql.Connection) {
  def runRaw(sql: String) = {
    val statement: Statement = connection.createStatement()
    try statement.execute(sql)
    finally statement.close()
  }
  def run[E[_] <: Expr[_], V[_[_]]](query: Query[V[E]])(implicit rw: upickle.default.ReadWriter[V[Id]]) = {

    val statement: Statement = connection.createStatement()
    val resultSet: ResultSet = statement.executeQuery(query.toSqlQuery)

    val res = collection.mutable.Buffer.empty[V[Id]]
    try {
      while (resultSet.next()) {
        val obj = ujson.Obj()
        val meta = resultSet.getMetaData
        for (i <- Range(0, meta.getColumnCount)) {
          obj(meta.getColumnName(i + 1).toLowerCase) = resultSet.getString(i + 1)
        }
        res.append(upickle.default.read[V[Id]](obj))
      }
    } finally {
      resultSet.close()
      statement.close()
    }
    res
  }
}
