package usql

import java.sql.{ResultSet, Statement}
import scala.util.DynamicVariable

object DatabaseApi{
  val tableNameMapper = new DynamicVariable[String => String](identity)
  val columnNameMapper = new DynamicVariable[String => String](identity)
}
class DatabaseApi(connection: java.sql.Connection,
                  tableNameMapper: String => String = identity,
                  tableNameUnMapper: String => String = identity,
                  columnNameMapper: String => String = identity,
                  columnNameUnMapper: String => String = identity) {
  def runRaw(sql: String) = {
    val statement: Statement = connection.createStatement()
    try statement.execute(sql)
    finally statement.close()
  }

  def toSqlQuery(query: Query[_], jsonQuery: ujson.Value, tableNames: Seq[String]): String = {
    val flatQuery = FlatJson.flatten(jsonQuery)
    val exprStr = flatQuery.map { case (k, v) => s"""$v as ${tableNameMapper(k)}""" }.mkString(", ")

    val tables = tableNames.mkString(", ")
    val filtersOpt =
      if (query.filter.isEmpty) ""
      else " WHERE " + query.filter.flatMap(_.toAtomics).map(_.toSqlExpr).mkString(" AND ")

    s"SELECT $exprStr FROM $tables$filtersOpt"
  }

  def run[T, V](query: Query[T])
               (implicit qr: Queryable[T, V]) = {
    DatabaseApi.columnNameMapper.withValue(columnNameMapper) {
      DatabaseApi.tableNameMapper.withValue(tableNameMapper) {
        val statement: Statement = connection.createStatement()
        val jsonQuery = OptionPickler.writeJs(query.expr)(qr.queryWriter)

        val queryStr = toSqlQuery(
          query,
          jsonQuery,
          qr.toTables(query.expr).map(t => tableNameMapper(t.tableName)).toSeq
        )

        val resultSet: ResultSet = statement.executeQuery(queryStr)

        val res = collection.mutable.Buffer.empty[V]
        try {
          while (resultSet.next()) {
            val kvs = collection.mutable.Buffer.empty[(String, ujson.Value)]
            val meta = resultSet.getMetaData

            for (i <- Range(0, meta.getColumnCount)) {
              val v = resultSet.getString(i + 1) match{
                case null => ujson.Null
                case s => ujson.Str(s)
              }
              kvs.append(meta.getColumnLabel(i + 1).toLowerCase -> v)
            }

            val json = FlatJson.unflatten(kvs.toSeq)

            def unMapJson(j: ujson.Value): ujson.Value = j match{
              case ujson.Obj(kvs) => ujson.Obj.from(kvs.map{case (k, v) => (columnNameUnMapper(k), unMapJson(v))})
              case j => j
            }

            val unMappedJson = unMapJson(json)

            res.append(OptionPickler.read[V](unMappedJson)(qr.valueReader))
          }
        } finally {
          resultSet.close()
          statement.close()
        }
        res
      }
    }
  }
}
