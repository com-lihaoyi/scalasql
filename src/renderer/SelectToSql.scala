package usql.renderer

import SqlStr.SqlStringSyntax
import usql.query.{Expr, Select}
import usql.{FlatJson, Queryable}

object SelectToSql {

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


  def joinsToSqlStr(joins: Seq[Select.Join],
                    fromSelectables: Map[Select.From, (Map[Expr[_], SqlStr], SqlStr)])
                   (implicit ctx: Context) = {
    SqlStr.join(
      joins.map { join =>
        val joinPrefix = SqlStr.opt(join.prefix)(s => usql" ${SqlStr.raw(s)} ")
        val joinSelectables = SqlStr.join(
          join.from.map { jf => fromSelectables(jf.from)._2 + SqlStr.opt(jf.on)(on => usql" ON $on") }
        )

        usql"$joinPrefix JOIN $joinSelectables"
      }
    )
  }

  def toSqlQuery0[Q, R](query: Select[Q],
                        qr: Queryable[Q, R],
                        tableNameMapper: String => String,
                        columnNameMapper: String => String): (Map[Expr[_], SqlStr], SqlStr) = {
    val (namedFromsMap, fromSelectables, exprNaming, ctx) = computeContext(
      tableNameMapper,
      columnNameMapper,
      query.from ++ query.joins.flatMap(_.from.map(_.from)),
      None
    )

    implicit val context: Context = ctx

    val (flattenedExpr, exprStr) = sqlExprsStr(query.expr, qr, context)

    val tables = SqlStr.join(query.from.map(fromSelectables(_)._2), usql", ")

    val joins = joinsToSqlStr(query.joins, fromSelectables)

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
        case Some(Select.AscDesc.Asc) => usql" ASC"
        case Some(Select.AscDesc.Desc) => usql" DESC"
      }

      val nulls = SqlStr.opt(orderBy.nulls){
        case Select.Nulls.First => usql" NULLS FIRST"
        case Select.Nulls.Last => usql" NULLS LAST"
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
                     selectables: Seq[Select.From],
                     updateTable: Option[Select.TableRef]) = {
    val namedFromsMap0 = selectables
      .zipWithIndex
      .map {
        case (t: Select.TableRef, i) => (t, tableNameMapper(t.value.tableName) + i)
        case (s: Select.SubqueryRef[_], i) => (s, "subquery" + i)
      }
      .toMap

    val namedFromsMap = namedFromsMap0 ++ updateTable.map(t => t -> tableNameMapper(t.value.tableName))

    def computeSelectable(t: Select.From) = t match {
      case t: Select.TableRef =>
        (Map.empty[Expr[_], SqlStr], SqlStr.raw(tableNameMapper(t.value.tableName)) + usql" " + SqlStr.raw(namedFromsMap(t)))

      case t: Select.SubqueryRef[_] =>
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
