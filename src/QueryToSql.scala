package usql

import usql.SqlStr.SqlStringSyntax

import scala.util.DynamicVariable

object QueryToSql {
  val tableNameMapper = new DynamicVariable[String => String](identity)
  val columnNameMapper = new DynamicVariable[String => String](identity)
  val fromNaming = new DynamicVariable[Map[Query.From, String]](Map())

  def toSqlQuery[T, V](query: Query[T],
                       qr: Queryable[T, V],
                       tableNameMapper: String => String = identity,
                       columnNameMapper: String => String = identity): SqlStr = {
    QueryToSql.columnNameMapper.withValue(columnNameMapper) {
      QueryToSql.tableNameMapper.withValue(tableNameMapper) {
        toSqlQuery0(query, qr)
      }
    }
  }

  def toSqlQuery0[T, V](query: Query[T], qr: Queryable[T, V]): SqlStr = {
    val namedFroms = (query.from ++ query.joins.flatMap(_.from).map(_.from)).zipWithIndex.map {
      case (t: Query.TableRef, i) => (t, tableNameMapper.value(t.value.tableName) + i)
      case (s: Query.SubqueryRef[_], i) => (s, "subquery" + i)
    }
    val namedFromsMap = namedFroms.toMap

    QueryToSql.fromNaming.withValue(namedFroms.toMap) {
      val jsonQuery = OptionPickler.writeJs(query.expr)(qr.queryWriter)

      val flatQuery = FlatJson.flatten(jsonQuery)
      val exprStr = SqlStr.join(
        flatQuery.map {
          case (k, v) => v + usql" as " + SqlStr.raw(tableNameMapper.value(k))
        },
        usql", "
      )

      def optional[T](t: Option[T])(f: T => SqlStr) = t.map(f).getOrElse(usql"")

      def optionalSeq[T](t: Seq[T])(f: Seq[T] => SqlStr) = if (t.nonEmpty) f(t) else usql""

      def selectable(t: Query.From) = t match{
        case t: Query.TableRef =>
          SqlStr.raw(tableNameMapper.value(t.value.tableName)) + usql" " + SqlStr.raw(namedFromsMap(t))

        case t: Query.SubqueryRef[_] =>
          toSqlQuery0(t.value, t.qr) + usql" " + SqlStr.raw(namedFromsMap(t))
      }
      val tables = SqlStr.join(query.from.map(selectable), usql", ")

      val joins = SqlStr.join(
        query.joins.map{join =>
          optional(join.prefix)(s => usql" " + SqlStr.raw(s) + usql" ") +
            usql" JOIN " +
            SqlStr.join(
              join.from.map{ jf =>
                selectable(jf.from) + optional(jf.on)(on => usql" ON " + on.toSqlExpr)
              }
            )
        }
      )

      val filtersOpt = optionalSeq(query.where) { where =>
        usql" WHERE " + SqlStr.join(where.map(_.toSqlExpr), usql" AND ")
      }

      val sortOpt = optional(query.orderBy) { orderBy =>
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

      val limitOpt = optional(query.limit) { limit =>
        usql" LIMIT " + SqlStr.raw(limit.toString)
      }

      val offsetOpt = optional(query.offset) { offset =>
        usql" OFFSET " + SqlStr.raw(offset.toString)
      }

      usql"SELECT " + exprStr + usql" FROM " + tables + joins + filtersOpt + sortOpt + limitOpt + offsetOpt
    }
  }

}
