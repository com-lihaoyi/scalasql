package usql

import java.sql.{ResultSet, Statement}


class DatabaseApi(connection: java.sql.Connection) {
  def runRaw(sql: String) = {
    val statement: Statement = connection.createStatement()
    try statement.execute(sql)
    finally statement.close()
  }

  def toSqlQuery(query: Query[_], jsonQuery: ujson.Value, tableNames: Seq[String]): String = {
    val flatQuery = FlatJson.flatten(jsonQuery)
    val exprStr = flatQuery.map { case (k, v) => s"""$v as $k""" }.mkString(", ")

    val tables = tableNames.mkString(", ")
    val filtersOpt =
      if (query.filter.isEmpty) ""
      else " WHERE " + query.filter.flatMap(_.toAtomics).map(_.toSqlExpr).mkString(" AND ")

    s"SELECT $exprStr FROM $tables$filtersOpt"
  }

  def run[T, V](query: Query[T])
               (implicit qr: Queryable[T, V]) = {

    val statement: Statement = connection.createStatement()
    val jsonQuery = upickle.default.writeJs(query.expr)(qr.queryWriter)

    val queryStr = toSqlQuery(query, jsonQuery, qr.toTables(query.expr).map(_.tableName).toSeq)
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

        res.append(upickle.default.read[V](json)(qr.valueReader))
      }
    } finally {
      resultSet.close()
      statement.close()
    }
    res
  }
}
