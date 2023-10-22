package usql.renderer

import SqlStr.SqlStringSyntax
import usql.query.{
  AscDesc,
  CompoundSelect,
  Expr,
  From,
  Join,
  Joinable,
  Nulls,
  SimpleSelect,
  SubqueryRef,
  TableRef
}
import usql.Queryable
import usql.utils.FlatJson

object SelectToSql {

  def joinsToSqlStr(
      joins: Seq[Join],
      fromSelectables: Map[From, (Map[Expr.Identity, SqlStr], SqlStr)]
  )(implicit ctx: Context) = {
    SqlStr.join(
      joins.map { join =>
        val joinPrefix = SqlStr.opt(join.prefix)(s => usql" ${SqlStr.raw(s)} ")
        val joinSelectables = SqlStr.join(
          join.from.map { jf =>
            fromSelectables(jf.from)._2 + SqlStr.opt(jf.on)(on => usql" ON $on")
          }
        )

        usql"$joinPrefix JOIN $joinSelectables"
      }
    )
  }

  def apply[Q, R](
      query: Joinable[Q, R],
      qr: Queryable[Q, R],
      tableNameMapper: String => String,
      columnNameMapper: String => String,
      previousFromMapping: Map[From, String]
  ): (Map[Expr.Identity, SqlStr], SqlStr, Context) = {
    query match {
      case q: SimpleSelect[Q, R] =>
        simple(q, qr, tableNameMapper, columnNameMapper, previousFromMapping)
      case q: CompoundSelect[Q, R] =>
        compound(q, qr, tableNameMapper, columnNameMapper, previousFromMapping)
    }
  }

  def compound[Q, R](
      query: CompoundSelect[Q, R],
      qr: Queryable[Q, R],
      tableNameMapper: String => String,
      columnNameMapper: String => String,
      previousFromMapping: Map[From, String]
  ): (Map[Expr.Identity, SqlStr], SqlStr, Context) = {
    val (lhsMap, lhsStr0, context) =
      apply(query.lhs, qr, tableNameMapper, columnNameMapper, previousFromMapping)

    val lhsStr = if (query.lhs.isInstanceOf[CompoundSelect[_, _]]) usql"($lhsStr0)" else lhsStr0
    implicit val ctx = context

    val compound = SqlStr.optSeq(query.compoundOps) { compoundOps =>
      val compoundStrs = compoundOps.map { op =>
        val (compoundMapping, compoundStr, compoundCtx) =
          apply(op.rhs, qr, tableNameMapper, columnNameMapper, previousFromMapping)

        usql" ${SqlStr.raw(op.op)} $compoundStr"
      }

      SqlStr.join(compoundStrs)
    }

    val newCtx = new Context(
      context.fromNaming,
      context.exprNaming ++ lhsMap,
      context.tableNameMapper,
      context.columnNameMapper
    )

    val sortOpt = SqlStr.opt(query.orderBy) { orderBy =>
      val ascDesc = orderBy.ascDesc match {
        case None => usql""
        case Some(AscDesc.Asc) => usql" ASC"
        case Some(AscDesc.Desc) => usql" DESC"
      }

      val nulls = SqlStr.opt(orderBy.nulls) {
        case Nulls.First => usql" NULLS FIRST"
        case Nulls.Last => usql" NULLS LAST"
      }

      usql" ORDER BY " + orderBy.expr.toSqlQuery(newCtx) + ascDesc + nulls
    }

    val limitOpt = SqlStr.opt(query.limit) { limit =>
      usql" LIMIT " + SqlStr.raw(limit.toString)
    }

    val offsetOpt = SqlStr.opt(query.offset) { offset =>
      usql" OFFSET " + SqlStr.raw(offset.toString)
    }

    val res = lhsStr + compound + sortOpt + limitOpt + offsetOpt

    (lhsMap, res, context)
  }

  def simple[Q, R](
      query: SimpleSelect[Q, R],
      qr: Queryable[Q, R],
      tableNameMapper: String => String,
      columnNameMapper: String => String,
      previousFromMapping: Map[From, String]
  ): (Map[Expr.Identity, SqlStr], SqlStr, Context) = {
    val (namedFromsMap, fromSelectables, exprNaming, ctx) = computeContext(
      tableNameMapper,
      columnNameMapper,
      query.from ++ query.joins.flatMap(_.from.map(_.from)),
      None,
      previousFromMapping
    )

    implicit val context: Context = ctx

    val exprPrefix = SqlStr.opt(query.exprPrefix) { p => SqlStr.raw(p) + usql" " }
    val (flattenedExpr, exprStr) = ExprsToSql(qr.walk(query.expr), exprPrefix, context)

    val tables = SqlStr.join(query.from.map(fromSelectables(_)._2), usql", ")

    val joins = joinsToSqlStr(query.joins, fromSelectables)

    val filtersOpt = SqlStr.optSeq(query.where) { where =>
      usql" WHERE " + SqlStr.join(where.map(_.toSqlQuery), usql" AND ")
    }

    val groupByOpt = SqlStr.opt(query.groupBy0) { groupBy =>
      val havingOpt = SqlStr.optSeq(groupBy.having) { having =>
        usql" HAVING " + SqlStr.join(having.map(_.toSqlQuery), usql" AND ")
      }
      usql" GROUP BY ${groupBy.expr}${havingOpt}"
    }

    val jsonQueryMap = flattenedExpr
      .map { case (k, v) =>
        (
          v.exprIdentity,
          SqlStr.raw((FlatJson.basePrefix +: k).map(columnNameMapper).mkString(FlatJson.delimiter))
        )
      }
      .toMap

    (
      jsonQueryMap,
      exprStr + usql" FROM " + tables + joins + filtersOpt + groupByOpt,
      ctx
    )
  }

  def computeContext(
      tableNameMapper: String => String,
      columnNameMapper: String => String,
      selectables: Seq[From],
      updateTable: Option[TableRef],
      previousFromMapping: Map[From, String]
  ) = {
    val namedFromsMap0 = selectables
      .zipWithIndex
      .map {
        case (t: TableRef, i) => (t, tableNameMapper(t.value.tableName) + i)
        case (s: SubqueryRef[_, _], i) => (s, "subquery" + i)
        case x => throw new Exception("wtf " + x)
      }
      .toMap

    val namedFromsMap = previousFromMapping ++ namedFromsMap0 ++ updateTable.map(t =>
      t -> tableNameMapper(t.value.tableName)
    )

    def computeSelectable(t: From) = t match {
      case t: TableRef =>
        (
          Map.empty[Expr.Identity, SqlStr],
          SqlStr.raw(tableNameMapper(t.value.tableName)) + usql" " + SqlStr.raw(namedFromsMap(t))
        )

      case t: SubqueryRef[_, _] =>
        val (subNameMapping, sqlStr, _) =
          apply(t.value, t.qr, tableNameMapper, columnNameMapper, previousFromMapping)
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
