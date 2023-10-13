package usql

import usql.SqlStr.SqlStringSyntax

import scala.collection.immutable
import scala.util.DynamicVariable

object QueryToSql {
  class Context(val fromNaming: Map[Query.From, String],
                val exprNaming: Map[Expr[_], SqlStr],
                val tableNameMapper: String => String,
                val columnNameMapper: String => String)

  def sqlExprsStr[Q, R](expr: Q, qr: Queryable[Q, R], context: Context) = {
    sqlExprsStr0(expr, qr, context, usql"SELECT ")
  }
  def sqlExprsStr0[Q, R](expr: Q, qr: Queryable[Q, R], context: Context, prefix: SqlStr) = {
    val flattenedExpr = qr.walk(expr)
    FlatJson.flatten(flattenedExpr, context) match {
      case Seq((FlatJson.basePrefix, singleExpr)) if singleExpr.isCompleteQuery =>
        (flattenedExpr, singleExpr)

      case flatQuery =>

        val exprsStr = SqlStr.join(
          flatQuery.map {
            case (k, v) => usql"$v as ${SqlStr.raw(context.tableNameMapper(k))}"
          },
          usql", "
        )

        (flattenedExpr, prefix + exprsStr)
    }
  }

  def toSqlQuery0[Q, R](query: Query[Q],
                        qr: Queryable[Q, R],
                        tableNameMapper: String => String,
                        columnNameMapper: String => String): (Map[Expr[_], SqlStr], SqlStr) = {
    val (namedFromsMap, fromSelectables, exprNaming, ctx) = computeContext(
      tableNameMapper,
      columnNameMapper,
      query.from ++ query.joins.flatMap(_.from.map(_.from))
    )

    implicit val context: Context = ctx

    val (flattenedExpr, exprStr) = sqlExprsStr(query.expr, qr, context)

    val tables = SqlStr.join(query.from.map(fromSelectables(_)._2), usql", ")

    val joins = SqlStr.join(
      query.joins.map { join =>
        val joinPrefix = SqlStr.opt(join.prefix)(s => usql" ${SqlStr.raw(s)} ")
        val joinSelectables = SqlStr.join(
          join.from.map { jf => fromSelectables(jf.from)._2 + SqlStr.opt(jf.on)(on => usql" ON $on") }
        )

        usql"$joinPrefix JOIN $joinSelectables"
      }
    )

    val filtersOpt = SqlStr.optSeq(query.where) { where =>
      usql" WHERE " + SqlStr.join(where.map(_.toSqlExpr), usql" AND ")
    }

    val groupByOpt = SqlStr.opt(query.groupBy0) { groupBy =>
      val havingOpt = SqlStr.optSeq(groupBy.having){ having =>
        usql" HAVING " + SqlStr.join(having.map(_.toSqlExpr), usql" AND ")
      }
      usql" GROUP BY ${groupBy.expr}${havingOpt}"
    }

    val sortOpt = SqlStr.opt(query.orderBy) { orderBy =>
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

      usql" ORDER BY " + orderBy.expr.toSqlExpr(context) + ascDesc + nulls
    }

    val limitOpt = SqlStr.opt(query.limit) { limit =>
      usql" LIMIT " + SqlStr.raw(limit.toString)
    }

    val offsetOpt = SqlStr.opt(query.offset) { offset =>
      usql" OFFSET " + SqlStr.raw(offset.toString)
    }

    val jsonQueryMap = flattenedExpr
      .map{case (k, v) => (v, SqlStr.raw((FlatJson.basePrefix +: k).map(columnNameMapper).mkString(FlatJson.delimiter)))}
      .toMap

    (
      jsonQueryMap,
      exprStr + usql" FROM " + tables + joins + filtersOpt + groupByOpt + sortOpt + limitOpt + offsetOpt
    )
  }

  def computeContext(tableNameMapper: String => String,
                     columnNameMapper: String => String,
                     selectables: Seq[Query.From]) = {
    val namedFromsMap = selectables
      .zipWithIndex
      .map {
        case (t: Query.TableRef, i) => (t, tableNameMapper(t.value.tableName) + i)
        case (s: Query.SubqueryRef[_], i) => (s, "subquery" + i)
      }
      .toMap

    def computeSelectable(t: Query.From) = t match {
      case t: Query.TableRef =>
        (Nil, SqlStr.raw(tableNameMapper(t.value.tableName)) + usql" " + SqlStr.raw(namedFromsMap(t)))

      case t: Query.SubqueryRef[_] =>
        val (subNameMapping, sqlStr) = toSqlQuery0(t.value, t.qr, tableNameMapper, columnNameMapper)
        (subNameMapping, usql"($sqlStr) ${SqlStr.raw(namedFromsMap(t))}")
    }

    val fromSelectables = selectables
      .map(f => (f, computeSelectable(f)))
      .toMap

    val exprNaming = fromSelectables.flatMap { case (k, vs) =>
      vs._1.map { case (e, s) => (e, usql"${SqlStr.raw(namedFromsMap(k))}.$s") }
    }


    val ctx: Context = new Context(
      namedFromsMap,
      exprNaming,
      tableNameMapper,
      columnNameMapper
    )

    (namedFromsMap, fromSelectables, exprNaming, ctx)
  }
}
