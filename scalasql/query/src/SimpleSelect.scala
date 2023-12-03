package scalasql.query

import scalasql.core.{
  Config,
  Context,
  DialectBase,
  SqlExprsToSql,
  FlatJson,
  JoinNullable,
  Queryable,
  Sql,
  SqlStr,
  TypeMapper,
  WithSqlExpr
}
import scalasql.renderer.JoinsToSql.joinsToSqlStr
import scalasql.core.SqlStr.{Renderable, SqlStringSyntax, join}
import scalasql.renderer.JoinsToSql

import scala.collection.mutable

/**
 * A `SELECT` query, with `FROM`/`JOIN`/`WHERE`/`GROUP BY`
 * clauses, but without `ORDER BY`/`LIMIT`/`TAKE`/`UNION` clauses
 */
class SimpleSelect[Q, R](
    val expr: Q,
    val exprPrefix: Option[Context => SqlStr],
    val from: Seq[Context.From],
    val joins: Seq[Join],
    val where: Seq[Sql[_]],
    val groupBy0: Option[GroupBy]
)(implicit val qr: Queryable.Row[Q, R], protected val dialect: DialectBase)
    extends Select[Q, R] {
  protected override def joinableToSelect = this

  protected def copy[Q, R](
      expr: Q = this.expr,
      exprPrefix: Option[Context => SqlStr] = this.exprPrefix,
      from: Seq[Context.From] = this.from,
      joins: Seq[Join] = this.joins,
      where: Seq[Sql[_]] = this.where,
      groupBy0: Option[GroupBy] = this.groupBy0
  )(implicit qr: Queryable.Row[Q, R]) =
    newSimpleSelect(expr, exprPrefix, from, joins, where, groupBy0)

  def selectWithExprPrefix(s: Context => SqlStr): Select[Q, R] = this.copy(exprPrefix = Some(s))

  def queryExpr[V: TypeMapper](
      f: Q => Context => SqlStr
  )(implicit qr2: Queryable.Row[Sql[V], V]): Sql[V] = {
    Sql[V] { implicit outerCtx: Context =>
      this
        .copy(expr = Sql[V] { implicit ctx: Context =>
          val newCtx = ctx.withFromNaming(outerCtx.fromNaming ++ ctx.fromNaming)

          f(expr)(newCtx)
        })
        .renderToSql(outerCtx)
        .withCompleteQuery(true)
    }
  }

  protected def selectToSimpleSelect() = this

  def map[Q2, R2](f: Q => Q2)(implicit qr: Queryable.Row[Q2, R2]): SimpleSelect[Q2, R2] =
    copy(expr = f(expr))

  def flatMap[Q2, R2](
      f: Q => FlatJoin.Rhs[Q2, R2]
  )(implicit qr2: Queryable.Row[Q2, R2]): Select[Q2, R2] = {
    def rec(
        thing: FlatJoin.Rhs[Q2, R2],
        joinOns: Seq[Join],
        wheres: Seq[Sql[Boolean]]
    ): Select[Q2, R2] = thing match {

      case other: FlatJoin.MapResult[Q, Q2, R, R2] =>
        val otherJoin = Join(other.prefix, Seq(Join.From(other.from, other.on)))
        joinCopy0(other.f, joinOns ++ Seq(otherJoin), other.where ++ wheres)

      case other: FlatJoin.FlatMapResult[Q, Q2, R, R2] =>
        val otherJoin = Join(other.prefix, Seq(Join.From(other.from, other.on)))
        rec(other.f, joinOns ++ Seq(otherJoin), wheres ++ other.where)
    }

    rec(f(expr), Nil, Nil)
  }

  def filter(f: Q => Sql[Boolean]): Select[Q, R] = {
    if (groupBy0.isEmpty) copy(where = where ++ Seq(f(expr)))
    else copy(groupBy0 = groupBy0.map(g => g.copy(having = g.having ++ Seq(f(expr)))))
  }

  def join0[Q2, R2, QF, RF](
      prefix: String,
      other: Joinable[Q2, R2],
      on: Option[(Q, Q2) => Sql[Boolean]]
  )(
      implicit ja: JoinAppend[Q, Q2, QF, RF]
  ): Select[QF, RF] = { joinCopy(other, on, prefix)(ja.appendTuple(_, _))(ja.qr) }

  protected def joinCopy0[Q3, R3](
      newExpr: Q3,
      newJoins: Seq[Join],
      newWheres: Seq[Sql[Boolean]]
  )(
      implicit jqr: Queryable.Row[Q3, R3]
  ): SimpleSelect[Q3, R3] = {
    // If this doesn't have a `groupBy` yet, then we can simply append another join. Otherwise
    // we have to wrap `this` in a subquery
    if (groupBy0.isEmpty) {
      copy(
        expr = newExpr,
        exprPrefix = exprPrefix,
        joins = joins ++ newJoins,
        where = where ++ newWheres
      )
    } else {
      subquery.copy(
        expr = newExpr,
        joins = newJoins,
        where = newWheres
      )
    }
  }

  def leftJoin[Q2, R2](other: Joinable[Q2, R2])(
      on: (Q, Q2) => Sql[Boolean]
  )(implicit joinQr: Queryable.Row[Q2, R2]): Select[(Q, JoinNullable[Q2]), (R, Option[R2])] = {
    joinCopy(other, Some(on), "LEFT JOIN")((e, o) => (e, JoinNullable(o)))
  }

  def rightJoin[Q2, R2](other: Joinable[Q2, R2])(
      on: (Q, Q2) => Sql[Boolean]
  )(implicit joinQr: Queryable.Row[Q2, R2]): Select[(JoinNullable[Q], Q2), (Option[R], R2)] = {
    joinCopy(other, Some(on), "RIGHT JOIN")((e, o) => (JoinNullable(e), o))
  }

  def outerJoin[Q2, R2](other: Joinable[Q2, R2])(on: (Q, Q2) => Sql[Boolean])(
      implicit joinQr: Queryable.Row[Q2, R2]
  ): Select[(JoinNullable[Q], JoinNullable[Q2]), (Option[R], Option[R2])] = {
    joinCopy(other, Some(on), "FULL OUTER JOIN")((e, o) => (JoinNullable(e), JoinNullable(o)))
  }

  def aggregate[E, V](f: SelectProxy[Q] => E)(implicit qr: Queryable.Row[E, V]): Aggregate[E, V] = {
    val selectProxyExpr = f(new SelectProxy[Q](expr))
    val copied = this.copy(expr = selectProxyExpr)
    new Aggregate[E, V](
      implicit ctx => copied.renderToSql(ctx),
      r => Query.construct(copied, r).head,
      selectProxyExpr
    )(qr)
  }

  def mapAggregate[Q2, R2](
      f: (Q, SelectProxy[Q]) => Q2
  )(implicit qr: Queryable.Row[Q2, R2]): Select[Q2, R2] = {
    val selectProxyExpr = f(expr, new SelectProxy[Q](expr))
    this.copy(expr = selectProxyExpr)
  }

  def groupBy[K, V, R1, R2](groupKey: Q => K)(
      groupAggregate: SelectProxy[Q] => V
  )(implicit qrk: Queryable.Row[K, R1], qrv: Queryable.Row[V, R2]): Select[(K, V), (R1, R2)] = {
    val groupKeyValue = groupKey(expr)
    val Seq(groupKeyExpr) = qrk.walkExprs(groupKeyValue)
    val newExpr = (groupKeyValue, groupAggregate(new SelectProxy[Q](this.expr)))

    // Weird hack to store the post-groupby `Select` as part of the `GroupBy`
    // object, because `.flatMap` sometimes need us to roll back any subsequent
    // `.map`s (???)
    lazy val groupByOpt: Option[GroupBy] = Some(GroupBy(groupKeyExpr, () => res, Nil))
    lazy val res =
      if (groupBy0.isEmpty) this.copy(expr = newExpr, groupBy0 = groupByOpt)
      else
        copy(
          expr = newExpr,
          exprPrefix = exprPrefix,
          from = Seq(this.subqueryRef),
          joins = Nil,
          where = Nil,
          groupBy0 = groupByOpt
        )
    res
  }

  def sortBy(f: Q => Sql[_]) = {
    newCompoundSelect(this, Nil, Seq(OrderBy(f(expr), None, None)), None, None)
  }

  def asc = throw new Exception(".asc must follow .sortBy")
  def desc = throw new Exception(".desc must follow .sortBy")
  def nullsFirst = throw new Exception(".nullsFirst must follow .sortBy")
  def nullsLast = throw new Exception(".nullsLast must follow .sortBy")

  def compound0(op: String, other: Select[Q, R]) = {
    val op2 = CompoundSelect.Op(op, Select.toSimpleFrom(other))
    newCompoundSelect(this, Seq(op2), Nil, None, None)
  }

  def drop(n: Int) = newCompoundSelect(this, Nil, Nil, None, Some(n))
  def take(n: Int) = newCompoundSelect(this, Nil, Nil, Some(n), None)

  protected def selectRenderer(prevContext: Context): SimpleSelect.Renderer[_, _] =
    new SimpleSelect.Renderer(this, prevContext)

  protected def selectLhsMap(prevContext: Context): Map[Sql.Identity, SqlStr] = {

    lazy val flattenedExpr = qr.walkLabelsAndExprs(expr)

    lazy val jsonQueryMap = flattenedExpr.map { case (k, v) =>
      val str = Config.joinName(k.map(prevContext.config.columnNameMapper), prevContext.config)
      val exprId = Sql.identity(v)

      (exprId, SqlStr.raw(str, Array(exprId)))
    }.toMap

    jsonQueryMap
  }

  protected def joinCopy[Q2, R2, Q3, R3](
      other: Joinable[Q2, R2],
      on: Option[(Q, Q2) => Sql[Boolean]],
      joinPrefix: String
  )(f: (Q, Q2) => Q3)(implicit jqr: Queryable.Row[Q3, R3]) = {

    val (otherJoin, otherSelect) = joinInfo(joinPrefix, other, on)

    joinCopy0(f(expr, WithSqlExpr.get(otherSelect)), otherJoin, Nil)(jqr)
  }

  override protected def queryConstruct(args: Queryable.ResultSetIterator): Seq[R] = {
    Seq(qr.construct(args))
  }
}

object SimpleSelect {

  def joinCopy[Q, R, Q2, R2, Q3, R3](
      self: SimpleSelect[Q, R],
      other: Joinable[Q2, R2],
      on: Option[(Q, Q2) => Sql[Boolean]],
      joinPrefix: String
  )(f: (Q, Q2) => Q3)(implicit jqr: Queryable.Row[Q3, R3]) = {
    self.joinCopy(other, on, joinPrefix)(f)
  }
  def getRenderer(s: SimpleSelect[_, _], prevContext: Context): SimpleSelect.Renderer[_, _] =
    s.selectRenderer(prevContext)
  class Renderer[Q, R](query: SimpleSelect[Q, R], prevContext: Context)
      extends SelectBase.Renderer {
    lazy val flattenedExpr = query.qr.walkLabelsAndExprs(query.expr)
    lazy val froms = query.from ++ query.joins.flatMap(_.from.map(_.from))
    implicit lazy val context = Context.compute(prevContext, froms, None)

    lazy val joinOns =
      query.joins.map(_.from.map(_.on.map(t => SqlStr.flatten(Renderable.toSql(t)))))

    lazy val exprsStrs = {
      FlatJson.flatten(flattenedExpr, context).map { case (k, v) =>
        sql"$v AS ${SqlStr.raw(context.config.tableNameMapper(k))}"
      }
    }

    lazy val filtersOpt = SqlStr.flatten(SqlExprsToSql.booleanExprs(sql" WHERE ", query.where))

    lazy val groupByOpt = SqlStr.flatten(SqlStr.opt(query.groupBy0) { groupBy =>
      val havingOpt = SqlExprsToSql.booleanExprs(sql" HAVING ", groupBy.having)
      sql" GROUP BY ${groupBy.key}${havingOpt}"
    })

    def render(liveExprs: Option[Set[Sql.Identity]]) = {
      val exprStr = SqlStr.flatten(
        SqlStr.join(
          flattenedExpr.iterator.zip(exprsStrs).collect {
            case ((l, e), s) if liveExprs.fold(true)(_.contains(Sql.identity(e))) => s
          },
          SqlStr.commaSep
        )
      )

      val innerLiveExprs =
        exprStr.referencedExprs.toSet ++
          filtersOpt.referencedExprs ++
          groupByOpt.referencedExprs ++
          joinOns.flatMap(_.flatMap(_.toSeq.flatMap(_.referencedExprs)))

      val (renderedFroms, joins) = JoinsToSql.renderLateralJoins(
        prevContext,
        query.from,
        innerLiveExprs,
        query.joins,
        joinOns
      )

      lazy val exprPrefix = SqlStr.opt(query.exprPrefix) { p => p(context) + sql" " }

      val tables = SqlStr
        .join(query.from.map(renderedFroms(_)), SqlStr.commaSep)

      sql"SELECT " + exprPrefix + exprStr + sql" FROM " + tables + joins + filtersOpt + groupByOpt
    }

  }
}
