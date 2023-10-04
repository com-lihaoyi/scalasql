package usql

import java.sql.{ResultSet, Statement}
import scala.util.DynamicVariable
import SqlString.SqlStringSyntax

object DatabaseApi{
  val tableNameMapper = new DynamicVariable[String => String](identity)
  val columnNameMapper = new DynamicVariable[String => String](identity)
  val fromNaming = new DynamicVariable[Map[Query.From, String]](Map())
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

  def toSqlQuery[T, V](query: Query[T], qr: Queryable[T, V]): SqlString = {
    val froms = Seq(query.from) ++ query.joins.map(_.from)
    val namedFroms = froms.zipWithIndex.map {
      case (t: Query.TableRef, i) => (t, tableNameMapper(t.value.tableName) + i)
      case (s: Query.SubqueryRef[_], i) => (s, "subquery" + i)
    }

    DatabaseApi.fromNaming.withValue(namedFroms.toMap){
      val jsonQuery = OptionPickler.writeJs(query.expr)(qr.queryWriter)

      val flatQuery = FlatJson.flatten(jsonQuery)
      val exprStr = SqlString.join(
        flatQuery.map {
          case (k, v) => v + usql" as " + SqlString.raw(tableNameMapper(k))
        },
        usql", "
      )

      val tables = SqlString.join(
        namedFroms.map {
          case (t: Query.TableRef, name) =>
            SqlString.raw(tableNameMapper(t.value.tableName)) + usql" " + SqlString.raw(name)

          case (t: Query.SubqueryRef[_], name) =>
            toSqlQuery(t.value, t.qr) + usql" " + SqlString.raw(name)
        },
        usql", "
      )

      def optional[T](t: Option[T])(f: T => SqlString) = t.map(f).getOrElse(usql"")
      def optionalSeq[T](t: Seq[T])(f: Seq[T] => SqlString) = if (t.nonEmpty) f(t) else usql""

      val filtersOpt = optionalSeq(query.where){ where =>
        usql" WHERE " + SqlString.join(where.map(_.toSqlExpr), usql" AND ")
      }

      val sortOpt = optional(query.orderBy){ orderBy =>
        val ascDesc = orderBy.ascDesc match {
          case None => usql""
          case Some(Query.AscDesc.Asc) => usql" ASC"
          case Some(Query.AscDesc.Desc) => usql" DESC"
        }

        val nulls = orderBy.nulls match {
          case None => usql""
          case Some(Query.Nulls.First) => usql" NULLS FIRST"
          case Some(Query.Nulls.Last) => usql" NULLS LAST"
        }

        usql" ORDER BY " + orderBy.expr.toSqlExpr + ascDesc + nulls
      }

      val limitOpt = optional(query.limit){ limit =>
         usql" LIMIT " + SqlString.raw(limit.toString)
      }

      val offsetOpt = optional(query.offset){ offset =>
        usql" OFFSET " + SqlString.raw(offset.toString)
      }

      usql"SELECT " + exprStr + usql" FROM " + tables + filtersOpt + sortOpt + limitOpt + offsetOpt
    }
  }

  def run[T, V](query: Query[T])
               (implicit qr: Queryable[T, V]) = {
    DatabaseApi.columnNameMapper.withValue(columnNameMapper) {
      DatabaseApi.tableNameMapper.withValue(tableNameMapper) {

        val querySqlStr = toSqlQuery(query, qr)

        val queryStr = querySqlStr.queryParts.mkString("?")

        val statement = connection.prepareStatement(queryStr)

        for((p, n) <- querySqlStr.params.zipWithIndex) p match{
          case Interp.StringInterp(s) => statement.setString(n + 1, s)
          case Interp.IntInterp(i) => statement.setInt(n + 1, i)
          case Interp.DoubleInterp(d) => statement.setDouble(n + 1, d)
          case Interp.BooleanInterp(b) => statement.setBoolean(n + 1, b)
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
