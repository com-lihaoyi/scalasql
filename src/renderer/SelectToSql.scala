package scalasql.renderer

import SqlStr.SqlStringSyntax
import scalasql.query.{
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
import scalasql.{MappedType, Queryable}
import scalasql.utils.FlatJson

object SelectToSql {

  def joinsToSqlStr(
      joins: Seq[Join],
      fromSelectables: Map[From, (Map[Expr.Identity, SqlStr], SqlStr)]
  )(implicit ctx: Context) = {
    SqlStr.join(
      joins.map { join =>
        val joinPrefix = SqlStr.opt(join.prefix)(s => sql" ${SqlStr.raw(s)} ")
        val joinSelectables = SqlStr.join(
          join.from.map { jf =>
            fromSelectables(jf.from)._2 + SqlStr.opt(jf.on)(on => sql" ON $on")
          }
        )

        sql"$joinPrefix JOIN $joinSelectables"
      }
    )
  }

  def apply[Q, R](
      query: Joinable[Q, R],
      qr: Queryable[Q, R],
      context: Context
  ): (Map[Expr.Identity, SqlStr], SqlStr, Context, Seq[MappedType[_]]) = {
    query match {
      case q: SimpleSelect[Q, R] =>
        simple(q, qr, context)
      case q: CompoundSelect[Q, R] =>
        compound(q, qr, context)
    }
  }

  def compound[Q, R](
      query: CompoundSelect[Q, R],
      qr: Queryable[Q, R],
      prevContext: Context
  ): (Map[Expr.Identity, SqlStr], SqlStr, Context, Seq[MappedType[_]]) = {
    val (lhsMap, lhsStr0, context, mappedTypes) =
      apply(query.lhs, qr, prevContext)

    val lhsStr = if (query.lhs.isInstanceOf[CompoundSelect[_, _]]) sql"($lhsStr0)" else lhsStr0
    implicit val ctx = context

    val compound = SqlStr.optSeq(query.compoundOps) { compoundOps =>
      val compoundStrs = compoundOps.map { op =>
        val (compoundMapping, compoundStr, compoundCtx, compoundMappedTypes) =
          apply(op.rhs, qr, prevContext)

        sql" ${SqlStr.raw(op.op)} $compoundStr"
      }

      SqlStr.join(compoundStrs)
    }

    val newCtx = context.copy(exprNaming = context.exprNaming ++ lhsMap)

    val sortOpt = SqlStr.opt(query.orderBy) { orderBy =>
      val ascDesc = orderBy.ascDesc match {
        case None => sql""
        case Some(AscDesc.Asc) => sql" ASC"
        case Some(AscDesc.Desc) => sql" DESC"
      }

      val nulls = SqlStr.opt(orderBy.nulls) {
        case Nulls.First => sql" NULLS FIRST"
        case Nulls.Last => sql" NULLS LAST"
      }

      sql" ORDER BY " + orderBy.expr.toSqlQuery(newCtx)._1 + ascDesc + nulls
    }

    val limitOpt = SqlStr.opt(query.limit) { limit =>
      sql" LIMIT " + SqlStr.raw(limit.toString)
    }

    val offsetOpt = SqlStr.opt(query.offset) { offset =>
      sql" OFFSET " + SqlStr.raw(offset.toString)
    }

    val res = lhsStr + compound + sortOpt + limitOpt + offsetOpt

    (lhsMap, res, context, mappedTypes)
  }

  def simple[Q, R](
      query: SimpleSelect[Q, R],
      qr: Queryable[Q, R],
      prevContext: Context
  ): (Map[Expr.Identity, SqlStr], SqlStr, Context, Seq[MappedType[_]]) = {
    val (namedFromsMap, fromSelectables, exprNaming, ctx) = computeContext(
      prevContext,
      query.from ++ query.joins.flatMap(_.from.map(_.from)),
      None
    )

    implicit val context: Context = ctx

    val exprPrefix = SqlStr.opt(query.exprPrefix) { p => SqlStr.raw(p) + sql" " }
    val (flattenedExpr, exprStr) = ExprsToSql(qr.walk(query.expr), exprPrefix, context)

    val tables = SqlStr.join(query.from.map(fromSelectables(_)._2), sql", ")

    val joins = joinsToSqlStr(query.joins, fromSelectables)

    val filtersOpt = SqlStr.optSeq(query.where) { where =>
      sql" WHERE " + SqlStr.join(where.map(_.toSqlQuery._1), sql" AND ")
    }

    val groupByOpt = SqlStr.opt(query.groupBy0) { groupBy =>
      val havingOpt = SqlStr.optSeq(groupBy.having) { having =>
        sql" HAVING " + SqlStr.join(having.map(_.toSqlQuery._1), sql" AND ")
      }
      sql" GROUP BY ${groupBy.expr}${havingOpt}"
    }

    val jsonQueryMap = flattenedExpr
      .map { case (k, v) =>
        (
          v.exprIdentity,
          SqlStr.raw((FlatJson.basePrefix +: k).map(prevContext.columnNameMapper).mkString(
            FlatJson.delimiter
          ))
        )
      }
      .toMap

    (
      jsonQueryMap,
      exprStr + sql" FROM " + tables + joins + filtersOpt + groupByOpt,
      ctx,
      flattenedExpr.map(_._2.mappedType)
    )
  }

  def computeContext(
      prevContext: Context,
      selectables: Seq[From],
      updateTable: Option[TableRef]
  ) = {
    val namedFromsMap0 = selectables
      .zipWithIndex
      .map {
        case (t: TableRef, i) => (t, prevContext.tableNameMapper(t.value.tableName) + i)
        case (s: SubqueryRef[_, _], i) => (s, "subquery" + i)
        case x => throw new Exception("wtf " + x)
      }
      .toMap

    val namedFromsMap = prevContext.fromNaming ++ namedFromsMap0 ++ updateTable.map(t =>
      t -> prevContext.tableNameMapper(t.value.tableName)
    )

    def computeSelectable(t: From) = t match {
      case t: TableRef =>
        (
          Map.empty[Expr.Identity, SqlStr],
          SqlStr.raw(prevContext.tableNameMapper(t.value.tableName)) + sql" " + SqlStr.raw(
            namedFromsMap(t)
          )
        )

      case t: SubqueryRef[_, _] =>
        val (subNameMapping, sqlStr, _, _) =
          apply(t.value, t.qr, prevContext)
        (subNameMapping, sql"($sqlStr) ${SqlStr.raw(namedFromsMap(t))}")
    }

    val fromSelectables = selectables
      .map(f => (f, computeSelectable(f)))
      .toMap

    val exprNaming = fromSelectables.flatMap { case (k, vs) =>
      vs._1.map { case (e, s) => (e, sql"${SqlStr.raw(namedFromsMap(k))}.$s") }
    }

    val ctx: Context = prevContext.copy(fromNaming = namedFromsMap, exprNaming = exprNaming)

    (namedFromsMap, fromSelectables, exprNaming, ctx)
  }
}
