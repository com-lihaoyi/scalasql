package scalasql.query

import scalasql.core.{
  Context,
  DialectTypeMappers,
  LiveExprs,
  Queryable,
  Expr,
  ExprsToSql,
  SqlStr,
  WithSqlExpr
}
import scalasql.core.SqlStr.SqlStringSyntax

/**
 * A SQL `WITH` clause
 */
class WithCte[Q, R](
    walked: Queryable.Walked,
    val lhs: Select[?, ?],
    val cteRef: WithCteRef,
    val rhs: Select[Q, R],
    val withPrefix: SqlStr = sql"WITH "
)(implicit val qr: Queryable.Row[Q, R], protected val dialect: DialectTypeMappers)
    extends Select.Proxy[Q, R] {

  override protected def expr = Joinable.toFromExpr(rhs)._2
  private def unprefixed = new WithCte(walked, lhs, cteRef, rhs, SqlStr.commaSep)

  protected def selectToSimpleSelect() = this.subquery

  override def map[Q2, R2](f: Q => Q2)(implicit qr2: Queryable.Row[Q2, R2]): Select[Q2, R2] = {
    new WithCte(walked, lhs, cteRef, rhs.map(f))
  }

  override def filter(f: Q => Expr[Boolean]): Select[Q, R] = {
    new WithCte(walked, rhs.filter(f), cteRef, rhs)
  }

  override def sortBy(f: Q => Expr[?]): Select[Q, R] =
    new WithCte(walked, lhs, cteRef, rhs.sortBy(f))

  override def drop(n: Int): Select[Q, R] = new WithCte(walked, lhs, cteRef, rhs.drop(n))
  override def take(n: Int): Select[Q, R] = new WithCte(walked, lhs, cteRef, rhs.take(n))

  override protected def selectRenderer(prevContext: Context): SubqueryRef.Wrapped.Renderer =
    new WithCte.Renderer(walked, withPrefix, this, prevContext)

  override protected def selectExprAliases(prevContext: Context) = {
    SubqueryRef.Wrapped.exprAliases(rhs, prevContext)
  }

  override protected def queryConstruct(args: Queryable.ResultSetIterator): Seq[R] =
    Query.construct(rhs, args)

}

object WithCte {
  class Proxy[Q, R](
      lhs: Select[Q, R],
      lhsSubQueryRef: WithCteRef,
      val qr: Queryable.Row[Q, R],
      protected val dialect: DialectTypeMappers
  ) extends Select.Proxy[Q, R] {

    override def joinableToFromExpr: (Context.From, Q) = {
      val otherFrom = lhsSubQueryRef
      (otherFrom, WithSqlExpr.get(lhs))
    }

    override protected def selectToSimpleSelect(): SimpleSelect[Q, R] = {
      Select.newSimpleSelect[Q, R](
        lhs,
        expr = WithSqlExpr.get(lhs),
        exprPrefix = None,
        exprSuffix = None,
        preserveAll = false,
        from = Seq(lhsSubQueryRef),
        joins = Nil,
        where = Nil,
        groupBy0 = None
      )(qr, dialect)
    }

    override def selectRenderer(prevContext: Context): SubqueryRef.Wrapped.Renderer =
      new SubqueryRef.Wrapped.Renderer {
        def render(liveExprs: LiveExprs): SqlStr = {
          SqlStr.raw(prevContext.fromNaming(lhsSubQueryRef))
        }
      }

    override private[scalasql] def renderSql(ctx: Context): SqlStr = {
      SqlStr.raw(ctx.fromNaming(lhsSubQueryRef))
    }
  }

  class Renderer[Q, R](
      walked: Queryable.Walked,
      withPrefix: SqlStr,
      query: WithCte[Q, R],
      prevContext: Context
  ) extends SubqueryRef.Wrapped.Renderer {
    def render(liveExprs: LiveExprs) = {
      val newExprNaming = ExprsToSql.selectColumnReferences(walked, prevContext)
      val newContext = Context.compute(prevContext, Seq(query.cteRef), None)
      val cteName = SqlStr.raw(newContext.fromNaming(query.cteRef))
      val leadingSpace = query.rhs match {
        case w: WithCte[Q, R] => SqlStr.empty
        case r => sql" "
      }

      val wrapped = SubqueryRef.Wrapped
        .renderer(
          query.rhs match {
            case w: WithCte[Q, R] => w.unprefixed
            case r => r
          },
          newContext
        )
        .render(liveExprs)

      val rhsSql = SqlStr.flatten(leadingSpace + wrapped)
      val rhsReferenced = LiveExprs.some(rhsSql.referencedExprs.toSet)
      val lhsSql =
        SubqueryRef.Wrapped.renderer(query.lhs, prevContext).render(rhsReferenced)

      val cteColumns = SqlStr.join(
        newExprNaming.collect { case (exprId, name) if rhsReferenced.isLive(exprId) => name },
        SqlStr.commaSep
      )

      sql"$withPrefix$cteName ($cteColumns) AS ($lhsSql)$rhsSql"
    }

  }
}
