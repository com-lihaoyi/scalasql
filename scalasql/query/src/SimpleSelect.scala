package scalasql.query

import scalasql.core.{
  Aggregatable,
  Context,
  DialectTypeMappers,
  Expr,
  ExprsToSql,
  JoinNullable,
  LiveExprs,
  Queryable,
  SqlStr,
  TypeMapper
}
import scalasql.core.SqlStr.{Renderable, SqlStringSyntax}
import scalasql.renderer.JoinsToSql

/**
 * A `SELECT` query, with `FROM`/`JOIN`/`WHERE`/`GROUP BY`
 * clauses, but without `ORDER BY`/`LIMIT`/`TAKE`/`UNION` clauses
 */
class SimpleSelect[Q, R](
    val expr: Q,
    val exprPrefix: Option[Context => SqlStr],
    val exprSuffix: Option[Context => SqlStr],
    val preserveAll: Boolean,
    val from: Seq[Context.From],
    val joins: Seq[Join],
    val where: Seq[Expr[?]],
    val groupBy0: Option[GroupBy]
)(implicit val qr: Queryable.Row[Q, R], protected val dialect: DialectTypeMappers)
    extends Select[Q, R] {

  protected def copy[Q, R](
      expr: Q = this.expr,
      exprPrefix: Option[Context => SqlStr] = this.exprPrefix,
      exprSuffix: Option[Context => SqlStr] = this.exprSuffix,
      preserveAll: Boolean = this.preserveAll,
      from: Seq[Context.From] = this.from,
      joins: Seq[Join] = this.joins,
      where: Seq[Expr[?]] = this.where,
      groupBy0: Option[GroupBy] = this.groupBy0
  )(implicit qr: Queryable.Row[Q, R]) =
    newSimpleSelect(expr, exprPrefix, exprSuffix, preserveAll, from, joins, where, groupBy0)

  def selectWithExprPrefix(preserveAll: Boolean, s: Context => SqlStr): Select[Q, R] =
    this.copy(exprPrefix = Some(s), preserveAll = preserveAll)

  def selectWithExprSuffix(preserveAll: Boolean, s: Context => SqlStr): Select[Q, R] =
    this.copy(exprSuffix = Some(s), preserveAll = preserveAll)

  def aggregateExpr[V: TypeMapper](
      f: Q => Context => SqlStr
  )(implicit qr2: Queryable.Row[Expr[V], V]): Expr[V] = {
    Expr[V] { implicit outerCtx: Context =>
      this
        .copy(expr = Expr[V] { implicit ctx: Context =>
          val newCtx = ctx.withFromNaming(outerCtx.fromNaming ++ ctx.fromNaming)

          f(expr)(newCtx)
        })
        .renderSql(outerCtx)
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
        wheres: Seq[Expr[Boolean]]
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

  def filter(f: Q => Expr[Boolean]): Select[Q, R] = {
    if (groupBy0.isEmpty) copy(where = where ++ Seq(f(expr)))
    else copy(groupBy0 = groupBy0.map(g => g.copy(having = g.having ++ Seq(f(expr)))))
  }

  def join0[Q2, R2, QF, RF](
      prefix: String,
      other: Joinable[Q2, R2],
      on: Option[(Q, Q2) => Expr[Boolean]]
  )(
      implicit ja: JoinAppend[Q, Q2, QF, RF]
  ): Select[QF, RF] = { joinCopy(other, on, prefix)(ja.appendTuple(_, _))(ja.qr) }

  protected def joinCopy0[Q3, R3](
      newExpr: Q3,
      newJoins: Seq[Join],
      newWheres: Seq[Expr[Boolean]]
  )(
      implicit jqr: Queryable.Row[Q3, R3]
  ): SimpleSelect[Q3, R3] = {
    // If this doesn't have a `groupBy` yet, then we can simply append another join. Otherwise
    // we have to wrap `this` in a subquery
    if (groupBy0.isEmpty) {
      copy(
        expr = newExpr,
        exprPrefix = exprPrefix,
        exprSuffix = exprSuffix,
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
      on: (Q, Q2) => Expr[Boolean]
  )(implicit joinQr: Queryable.Row[Q2, R2]): Select[(Q, JoinNullable[Q2]), (R, Option[R2])] = {
    joinCopy(other, Some(on), "LEFT JOIN")((e, o) => (e, JoinNullable(o)))
  }

  def rightJoin[Q2, R2](other: Joinable[Q2, R2])(
      on: (Q, Q2) => Expr[Boolean]
  )(implicit joinQr: Queryable.Row[Q2, R2]): Select[(JoinNullable[Q], Q2), (Option[R], R2)] = {
    joinCopy(other, Some(on), "RIGHT JOIN")((e, o) => (JoinNullable(e), o))
  }

  def outerJoin[Q2, R2](other: Joinable[Q2, R2])(on: (Q, Q2) => Expr[Boolean])(
      implicit joinQr: Queryable.Row[Q2, R2]
  ): Select[(JoinNullable[Q], JoinNullable[Q2]), (Option[R], Option[R2])] = {
    joinCopy(other, Some(on), "FULL OUTER JOIN")((e, o) => (JoinNullable(e), JoinNullable(o)))
  }

  def aggregate[E, V](
      f: Aggregatable.Proxy[Q] => E
  )(implicit qr: Queryable.Row[E, V]): Aggregate[E, V] = {
    val aggregateProxy = f(new Aggregatable.Proxy[Q](expr))
    val copied = this.copy(expr = aggregateProxy)
    new Aggregate[E, V](
      implicit ctx => copied.renderSql(ctx),
      r => Query.construct(copied, r).head,
      aggregateProxy,
      qr
    )
  }

  def mapAggregate[Q2, R2](
      f: (Q, Aggregatable.Proxy[Q]) => Q2
  )(implicit qr: Queryable.Row[Q2, R2]): Select[Q2, R2] = {
    val aggregateProxy = f(expr, new Aggregatable.Proxy[Q](expr))
    this.copy(expr = aggregateProxy)
  }

  def groupBy[K, V, R1, R2](groupKey: Q => K)(
      groupAggregate: Aggregatable.Proxy[Q] => V
  )(implicit qrk: Queryable.Row[K, R1], qrv: Queryable.Row[V, R2]): Select[(K, V), (R1, R2)] = {
    val groupKeysValue = groupKey(expr)
    val groupKeysExpr = qrk.walkExprs(groupKeysValue)
    val newExpr = (groupKeysValue, groupAggregate(new Aggregatable.Proxy[Q](this.expr)))

    // Weird hack to store the post-groupby `Select` as part of the `GroupBy`
    // object, because `.flatMap` sometimes need us to roll back any subsequent
    // `.map`s (???)
    lazy val groupByOpt: Option[GroupBy] = Some(GroupBy(groupKeysExpr, () => res, Nil))
    lazy val res =
      if (groupBy0.isEmpty) this.copy(expr = newExpr, groupBy0 = groupByOpt)
      else
        copy(
          expr = newExpr,
          exprPrefix = exprPrefix,
          exprSuffix = exprSuffix,
          from = Seq(this.subqueryRef),
          joins = Nil,
          where = Nil,
          groupBy0 = groupByOpt
        )
    res
  }

  def sortBy(f: Q => Expr[?]): Select[Q, R] = {
    newCompoundSelect(this, Nil, Seq(OrderBy(f(expr), None, None)), None, None)
  }

  def asc: Select[Q, R] = throw new Exception(".asc must follow .sortBy")
  def desc: Select[Q, R] = throw new Exception(".desc must follow .sortBy")
  def nullsFirst: Select[Q, R] = throw new Exception(".nullsFirst must follow .sortBy")
  def nullsLast: Select[Q, R] = throw new Exception(".nullsLast must follow .sortBy")

  def compound0(op: String, other: Select[Q, R]) = {
    val op2 = CompoundSelect.Op(op, Select.toSimpleFrom(other))
    newCompoundSelect(this, Seq(op2), Nil, None, None)
  }

  def drop(n: Int): Select[Q, R] = newCompoundSelect(this, Nil, Nil, None, Some(n))
  def take(n: Int): Select[Q, R] = newCompoundSelect(this, Nil, Nil, Some(n), None)

  protected def selectRenderer(prevContext: Context): SimpleSelect.Renderer[?, ?] =
    new SimpleSelect.Renderer(this, prevContext)

  protected def selectExprAliases(prevContext: Context) = {
    ExprsToSql.selectColumnReferences(qr.walkLabelsAndExprs(expr), prevContext)
  }

  protected def joinCopy[Q2, R2, Q3, R3](
      other: Joinable[Q2, R2],
      on: Option[(Q, Q2) => Expr[Boolean]],
      joinPrefix: String
  )(f: (Q, Q2) => Q3)(implicit jqr: Queryable.Row[Q3, R3]) = {

    val (otherJoin, otherExpr) = joinInfo(joinPrefix, other, on)

    joinCopy0(f(expr, otherExpr), otherJoin, Nil)(jqr)
  }

  override protected def queryConstruct(args: Queryable.ResultSetIterator): Seq[R] = {
    Seq(qr.construct(args))
  }

}

object SimpleSelect {

  def joinCopy[Q, R, Q2, R2, Q3, R3](
      self: SimpleSelect[Q, R],
      other: Joinable[Q2, R2],
      on: Option[(Q, Q2) => Expr[Boolean]],
      joinPrefix: String
  )(f: (Q, Q2) => Q3)(implicit jqr: Queryable.Row[Q3, R3]) = {
    self.joinCopy(other, on, joinPrefix)(f)
  }
  def getRenderer(s: SimpleSelect[?, ?], prevContext: Context): SimpleSelect.Renderer[?, ?] =
    s.selectRenderer(prevContext)
  class Renderer[Q, R](query: SimpleSelect[Q, R], prevContext: Context)
      extends SubqueryRef.Wrapped.Renderer {
    lazy val flattenedExpr = query.qr.walkLabelsAndExprs(query.expr)
    lazy val froms = query.from ++ query.joins.flatMap(_.from.map(_.from))
    implicit lazy val context: Context = Context.compute(prevContext, froms, None)

    lazy val joinOns =
      query.joins.map(_.from.map(_.on.map(t => SqlStr.flatten(Renderable.renderSql(t)))))

    lazy val exprsStrs = {
      ExprsToSql.selectColumnSql(flattenedExpr, context).map { case (k, v) =>
        sql"$v AS ${SqlStr.raw(context.config.tableNameMapper(k))}"
      }
    }

    lazy val filtersOpt = SqlStr.flatten(ExprsToSql.booleanExprs(sql" WHERE ", query.where))

    lazy val groupByOpt = SqlStr.flatten(SqlStr.opt(query.groupBy0) { groupBy =>
      val havingOpt = ExprsToSql.booleanExprs(sql" HAVING ", groupBy.having)
      val groupByJoined =
        SqlStr.join(groupBy.keys.map(x => Renderable.renderSql(x)(context)), SqlStr.commaSep)
      sql" GROUP BY ${groupByJoined}${havingOpt}"
    })

    def render(liveExprs0: LiveExprs) = {
      val liveExprs = if (query.preserveAll) LiveExprs.none else liveExprs0
      val exprStr = SqlStr.flatten(
        SqlStr.join(
          flattenedExpr.iterator.zip(exprsStrs).collect {
            case ((l, e), s) if liveExprs.isLive(Expr.identity(e)) => s
          },
          SqlStr.commaSep
        )
      )

      val innerLiveExprs = LiveExprs.some(
        exprStr.referencedExprs.toSet ++
          filtersOpt.referencedExprs ++
          groupByOpt.referencedExprs ++
          joinOns.flatMap(_.flatMap(_.toSeq.flatMap(_.referencedExprs)))
      )
      val (renderedFroms, joins) = JoinsToSql.renderLateralJoins(
        prevContext,
        query.from,
        innerLiveExprs,
        query.joins,
        joinOns
      )

      lazy val exprPrefix = SqlStr.opt(query.exprPrefix) { p => p(context) + sql" " }
      lazy val exprSuffix = SqlStr.opt(query.exprSuffix) { p => p(context) }

      val tables = SqlStr
        .join(query.from.map(renderedFroms(_)), SqlStr.commaSep)

      sql"SELECT " + exprPrefix + exprStr + sql" FROM " + tables + joins + filtersOpt + groupByOpt + exprSuffix
    }

  }
}
