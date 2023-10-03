package usql

import java.sql.{ResultSet, Statement}
import scala.util.DynamicVariable
import SqlString.SqlStringSyntax

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

  def toSqlQuery(query: Query[_], jsonQuery: ujson.Value, tableNames: Seq[String]): SqlString = {
    val flatQuery = FlatJson.flatten(jsonQuery)
    val exprStr = SqlString.join(
      flatQuery.map {
        case (k, v) => v ++ usql" as " ++ SqlString.raw(tableNameMapper(k))
      },
      usql", "
    )

    val tables = SqlString.join(tableNames.map(SqlString.raw), usql", ")
    val filtersOpt =
      if (query.where.isEmpty) usql""
      else {
        val clauses = SqlString.join(
          query
            .where
            .map(_.toSqlExpr)
            .toSeq,
          usql" AND "
        )

        usql" WHERE " ++ clauses
      }

    usql"SELECT " ++ exprStr ++ usql" FROM " ++ tables ++ filtersOpt
  }

  def run[T, V](query: Query[T])
               (implicit qr: Queryable[T, V]) = {
    DatabaseApi.columnNameMapper.withValue(columnNameMapper) {
      DatabaseApi.tableNameMapper.withValue(tableNameMapper) {

        val jsonQuery = OptionPickler.writeJs(query.expr)(qr.queryWriter)

        val querySqlStr = toSqlQuery(
          query,
          jsonQuery,
          (
            qr.toTables(query.expr).toSeq ++
            query.where.toSeq.flatMap(_.toTables)
          ).map(t => tableNameMapper(t.tableName)).distinct
        )

        val queryStr = querySqlStr.queryParts.mkString("?")

        val statement = connection.prepareStatement(queryStr)

        for((p, n) <- querySqlStr.params.zipWithIndex){
          p match{
            case Interp.StringInterp(s) => statement.setString(n + 1, s)
            case Interp.IntInterp(i) => statement.setInt(n + 1, i)
            case Interp.DoubleInterp(d) => statement.setDouble(n + 1, d)
            case Interp.BooleanInterp(b) => statement.setBoolean(n + 1, b)
          }
        }

        val resultSet: ResultSet = statement.executeQuery()

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
              case ujson.Arr(vs) => ujson.Arr(vs.map(unMapJson))
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
