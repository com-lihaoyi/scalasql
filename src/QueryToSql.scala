package usql

import usql.SqlStr.SqlStringSyntax

import scala.util.DynamicVariable

object QueryToSql {
  val tableNameMapper = new DynamicVariable[String => String](identity)
  val columnNameMapper = new DynamicVariable[String => String](identity)
  val fromNaming = new DynamicVariable[Map[Query.From, String]](Map())
  val exprNaming = new DynamicVariable[Map[Expr[_], SqlStr]](Map())

  def toSqlQuery[T, V](query: Query[T],
                       qr: Queryable[T, V],
                       tableNameMapper: String => String = identity,
                       columnNameMapper: String => String = identity): SqlStr = {
    QueryToSql.columnNameMapper.withValue(columnNameMapper) {
      QueryToSql.tableNameMapper.withValue(tableNameMapper) {
        toSqlQuery0(query, qr)._2
      }
    }
  }

  def toSqlQuery0[T, V](query: Query[T], qr: Queryable[T, V]): (Map[Expr[_], SqlStr], SqlStr) = {
    val namedFroms = (query.from ++ query.joins.flatMap(_.from).map(_.from)).zipWithIndex.map {
      case (t: Query.TableRef, i) => (t, tableNameMapper.value(t.value.tableName) + i)
      case (s: Query.SubqueryRef[_], i) => (s, "subquery" + i)
    }
    val namedFromsMap = namedFroms.toMap

    QueryToSql.fromNaming.withValue(namedFroms.toMap) {

      def computeSelectable(t: Query.From) = t match {
        case t: Query.TableRef =>
          (Nil, SqlStr.raw(tableNameMapper.value(t.value.tableName)) + usql" " + SqlStr.raw(namedFromsMap(t)))

        case t: Query.SubqueryRef[_] =>
          val (subNameMapping, sqlStr) = toSqlQuery0(t.value, t.qr)
          (subNameMapping, usql"(" + sqlStr + usql") " + SqlStr.raw(namedFromsMap(t)))
      }

      val fromSelectables:  Map[Query.From, (Iterable[(Expr[_], SqlStr)], SqlStr)] = (query.from ++ query.joins.flatMap(_.from.map(_.from)))
        .map(f => (f, computeSelectable(f)))
        .toMap

      exprNaming.withValue(
        fromSelectables.flatMap { case (k, vs) =>
          vs._1.map { case (e, s) => (e, SqlStr.raw(namedFromsMap(k)) + usql"." + s) }
        }
      ) {
        val jsonQuery = qr.walk(query.expr)

        val flatQuery = FlatJson.flatten(jsonQuery)
        val exprStr = SqlStr.join(
          flatQuery.map {
            case (k, v) => v + usql" as " + SqlStr.raw(tableNameMapper.value(k))
          },
          usql", "
        )

        def optional[T](t: Option[T])(f: T => SqlStr) = t.map(f).getOrElse(usql"")

        def optionalSeq[T](t: Seq[T])(f: Seq[T] => SqlStr) = if (t.nonEmpty) f(t) else usql""

        val tables = SqlStr.join(query.from.map(fromSelectables(_)._2), usql", ")

        val joins = SqlStr.join(
          query.joins.map { join =>
            optional(join.prefix)(s => usql" " + SqlStr.raw(s) + usql" ") +
              usql" JOIN " +
              SqlStr.join(
                join.from.map { jf =>
                  fromSelectables(jf.from)._2 + optional(jf.on)(on => usql" ON " + on.toSqlExpr)
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

        val jsonQueryMap = jsonQuery
          .map{case (k, v) => (v, SqlStr.raw(("res" +: k).map(columnNameMapper.value).mkString("__")))}
          .toMap

        (
          jsonQueryMap,
          usql"SELECT " + exprStr + usql" FROM " + tables + joins + filtersOpt + sortOpt + limitOpt + offsetOpt
        )
      }
    }
  }
}
