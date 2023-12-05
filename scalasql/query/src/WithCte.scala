package scalasql.query

import scalasql.core.{
  ColumnNamer,
  Context,
  DialectTypeMappers,
  LiveSqlExprs,
  Queryable,
  Sql,
  SqlExprsToSql,
  SqlStr,
  TypeMapper,
  WithSqlExpr
}
import scalasql.core.SqlStr.{Renderable, SqlStringSyntax}

/**
 * A SQL `WITH` clause
 */
class WithCte[Q, R](
    val lhs: Select[_, _],
    val lhsSubQuery: WithCteRef,
    val rhs: Select[Q, R],
    val withPrefix: SqlStr = sql"WITH "
)(implicit val qr: Queryable.Row[Q, R], protected val dialect: DialectTypeMappers)
    extends Select.Proxy[Q, R] {

  override protected def expr = WithSqlExpr.get(Joinable.toSelect(rhs))
  private def unprefixed = new WithCte(lhs, lhsSubQuery, rhs, SqlStr.commaSep)

  protected def selectToSimpleSelect() = this.subquery

  override def map[Q2, R2](f: Q => Q2)(implicit qr2: Queryable.Row[Q2, R2]): Select[Q2, R2] = {
    new WithCte(lhs, lhsSubQuery, rhs.map(f))
  }

  override def filter(f: Q => Sql[Boolean]): Select[Q, R] = {
    new WithCte(rhs.filter(f), lhsSubQuery, rhs)
  }

  override def sortBy(f: Q => Sql[_]) = new WithCte(lhs, lhsSubQuery, rhs.sortBy(f))

  override def drop(n: Int) = new WithCte(lhs, lhsSubQuery, rhs.drop(n))
  override def take(n: Int) = new WithCte(lhs, lhsSubQuery, rhs.take(n))

  override protected def selectRenderer(prevContext: Context) =
    new WithCte.Renderer(withPrefix, this, prevContext)

  override protected def selectLhsMap(prevContext: Context): Map[Sql.Identity, SqlStr] = {
    SelectBase.lhsMap(rhs, prevContext)
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
//    override def joinableSelect = this
    override def joinableIsTrivial = true
    protected override def joinableToSelect = selectToSimpleSelect()
    override protected def selectToSimpleSelect(): SimpleSelect[Q, R] = {
      Select.newSimpleSelect[Q, R](
        lhs,
        expr = WithSqlExpr.get(lhs),
        exprPrefix = None,
        preserveAll = false,
        from = Seq(lhsSubQueryRef),
        joins = Nil,
        where = Nil,
        groupBy0 = None
      )(qr, dialect)
    }

    override def selectRenderer(prevContext: Context): SelectBase.Renderer =
      new SelectBase.Renderer {
        def render(liveExprs: LiveSqlExprs): SqlStr = {
          SqlStr.raw(prevContext.fromNaming(lhsSubQueryRef))
        }
      }

    override protected def renderToSql(ctx: Context): SqlStr = {
      SqlStr.raw(ctx.fromNaming(lhsSubQueryRef))
    }
  }

  class Renderer[Q, R](withPrefix: SqlStr, query: WithCte[Q, R], prevContext: Context)
      extends SelectBase.Renderer {
    def render(liveExprs: LiveSqlExprs) = {
      val walked =
        query.lhs.qr
          .asInstanceOf[Queryable[Any, Any]]
          .walkLabelsAndExprs(WithSqlExpr.get(query.lhs))

      val newExprNaming = ColumnNamer.flattenCte(walked, prevContext)

      val newContext = Context.compute(prevContext, Seq(query.lhsSubQuery), None)
      val cteName = SqlStr.raw(newContext.fromNaming(query.lhsSubQuery))
      val rhsSql = SqlStr.flatten(
        (query.rhs match {
          case w: WithCte[Q, R] => SqlStr.empty
          case r => sql" "
        }) +
          SelectBase
            .renderer(
              query.rhs match {
                case w: WithCte[Q, R] => w.unprefixed
                case r => r
              },
              newContext.withExprNaming(
                newContext.exprNaming ++
                  newExprNaming.map { case (k, v) => (k, sql"$cteName.$v") }
              )
            )
            .render(liveExprs)
      )
      val rhsReferenced = LiveSqlExprs.some(rhsSql.referencedExprs.toSet)
      val lhsSql =
        SelectBase.renderer(query.lhs, prevContext).render(rhsReferenced)

      val cteColumns = SqlStr.join(
        newExprNaming.collect { case (exprId, name) if rhsReferenced.isLive(exprId) => name },
        SqlStr.commaSep
      )

      sql"$withPrefix$cteName ($cteColumns) AS ($lhsSql)$rhsSql"
    }

  }
}
