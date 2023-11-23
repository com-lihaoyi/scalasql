package scalasql.query

import scalasql.renderer.SqlStr.{Renderable, SqlStringSyntax}
import scalasql.renderer.{Context, ExprsToSql, SqlStr}
import scalasql.utils.{FlatJson, OptionPickler}
import scalasql.{Queryable, TypeMapper}

/**
 * A SQL `WITH` clause
 */
class WithCte[Q, R](
    val lhs: Select[_, _],
    val lhsSubQuery: WithCteRef[_, _],
    val rhs: Select[Q, R],
    val withPrefix: SqlStr = sql"WITH "
)(implicit val qr: Queryable.Row[Q, R])
    extends Select[Q, R] {

  protected def expr = WithExpr.get(Joinable.joinableSelect(rhs))
  private def unprefixed = new WithCte(lhs, lhsSubQuery, rhs, sql", ")


  def distinct: Select[Q, R] = selectSimpleFrom().distinct
  protected def selectSimpleFrom() = this.subquery
  def queryExpr[V: TypeMapper](f: Q => Context => SqlStr)(
      implicit qr: Queryable.Row[Expr[V], V]
  ): Expr[V] = selectSimpleFrom().queryExpr[V](f)

  def map[Q2, R2](f: Q => Q2)(implicit qr2: Queryable.Row[Q2, R2]): Select[Q2, R2] = {
    new WithCte(lhs, lhsSubQuery, rhs.map(f))
  }

  def flatMap[Q2, R2](f: Q => FlatJoin.Rhs[Q2, R2])(
      implicit qr: Queryable.Row[Q2, R2]
  ): Select[Q2, R2] = { selectSimpleFrom().flatMap(f) }

  def filter(f: Q => Expr[Boolean]): Select[Q, R] = {
    new WithCte(rhs.filter(f), lhsSubQuery, rhs)
  }

  def join0[Q2, R2](
      prefix: String,
      other: Joinable[Q2, R2],
      on: Option[(Q, Q2) => Expr[Boolean]]
  )(
      implicit joinQr: Queryable.Row[Q2, R2]
  ): Select[(Q, Q2), (R, R2)] = { selectSimpleFrom().join0(prefix, other, on) }

  def leftJoin[Q2, R2](other: Joinable[Q2, R2])(on: (Q, Q2) => Expr[Boolean])(
      implicit joinQr: Queryable.Row[Q2, R2]
  ): Select[(Q, JoinNullable[Q2]), (R, Option[R2])] = { selectSimpleFrom().leftJoin(other)(on) }

  def rightJoin[Q2, R2](other: Joinable[Q2, R2])(on: (Q, Q2) => Expr[Boolean])(
      implicit joinQr: Queryable.Row[Q2, R2]
  ): Select[(JoinNullable[Q], Q2), (Option[R], R2)] = { selectSimpleFrom().rightJoin(other)(on) }

  def outerJoin[Q2, R2](other: Joinable[Q2, R2])(on: (Q, Q2) => Expr[Boolean])(
      implicit joinQr: Queryable.Row[Q2, R2]
  ): Select[(JoinNullable[Q], JoinNullable[Q2]), (Option[R], Option[R2])] = {
    selectSimpleFrom().outerJoin(other)(on)
  }

  def aggregate[E, V](f: SelectProxy[Q] => E)(implicit qr: Queryable.Row[E, V]): Aggregate[E, V] = {
    selectSimpleFrom().aggregate(f)
  }

  def mapAggregate[Q2, R2](
      f: (Q, SelectProxy[Q]) => Q2
  )(implicit qr: Queryable.Row[Q2, R2]): Select[Q2, R2] = {
    selectSimpleFrom().mapAggregate(f)
  }

  def groupBy[K, V, R1, R2](groupKey: Q => K)(
      groupAggregate: SelectProxy[Q] => V
  )(implicit qrk: Queryable.Row[K, R1], qrv: Queryable.Row[V, R2]): Select[(K, V), (R1, R2)] = {
    selectSimpleFrom().groupBy(groupKey)(groupAggregate)
  }

  def sortBy(f: Q => Expr[_]) = new WithCte(lhs, lhsSubQuery, rhs.sortBy(f))

  def asc = throw new Exception(".asc must follow .sortBy")
  def desc = throw new Exception(".desc must follow .sortBy")
  def nullsFirst = throw new Exception(".nullsFirst must follow .sortBy")
  def nullsLast = throw new Exception(".nullsLast must follow .sortBy")

  def compound0(op: String, other: Select[Q, R]) = selectSimpleFrom().compound0(op, other)

  def drop(n: Int) = new WithCte(lhs, lhsSubQuery, rhs.drop(n))
  def take(n: Int) = new WithCte(lhs, lhsSubQuery, rhs.take(n))

  protected def queryValueReader = OptionPickler.SeqLikeReader2(qr.valueReader(expr), implicitly)

  protected def selectRenderer(prevContext: Context) =
    new WithCte.Renderer(withPrefix, this, prevContext)

  protected override def queryTypeMappers() = qr.toTypeMappers(expr)

  protected def selectLhsMap(prevContext: Context): Map[Expr.Identity, SqlStr] = {
    Select.selectLhsMap(lhs, prevContext)
  }
}

object WithCte {
  class Proxy[Q, R](lhs: WithExpr[Q], lhsSubQueryRef: WithCteRef[Q, R], val qr: Queryable.Row[Q, R]) extends Select.Proxy[Q, R] {
//    override def joinableSelect = this
    override def joinableIsTrivial = true
    protected override def joinableSelect = selectSimpleFrom()
    override protected def selectSimpleFrom(): SimpleSelect[Q, R] =
      new SimpleSelect[Q, R](
        expr = WithExpr.get(lhs),
        exprPrefix = None,
        from = Seq(lhsSubQueryRef),
        joins = Nil,
        where = Nil,
        groupBy0 = None
      )(qr)

    override def selectRenderer(prevContext: Context): Select.Renderer = new Select.Renderer {
      def render(liveExprs: Option[Set[Expr.Identity]]): SqlStr = {
        SqlStr.raw(prevContext.fromNaming(lhsSubQueryRef))
      }
    }

    override protected def renderToSql(ctx: Context): SqlStr = {
      SqlStr.raw(ctx.fromNaming(lhsSubQueryRef))
    }
  }

  class Renderer[Q, R](withPrefix: SqlStr, query: WithCte[Q, R], prevContext: Context) extends Select.Renderer {
    def render(liveExprs: Option[Set[Expr.Identity]]) = {
      val walked = query.lhs.qr.asInstanceOf[Queryable[Any, Any]].walk(WithExpr.get(query.lhs))
      val newExprNaming = walked.map { case (tokens, expr) =>
        (
          Expr.exprIdentity(expr),
          SqlStr.raw(FlatJson.flatten(tokens, prevContext), Seq(Expr.exprIdentity(expr)))
        )
      }

      val newContext = Context.compute(prevContext, Seq(query.lhsSubQuery), None)
      val cteName = SqlStr.raw(newContext.fromNaming(query.lhsSubQuery))
      val rhsSql = SqlStr.flatten(
        (query.rhs match {
          case w: WithCte[Q, R] => sql""
          case r => sql" "
        }) +
        Select
          .selectRenderer(
            query.rhs match{
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
      val rhsReferenced = rhsSql.referencedExprs.toSet
      val lhsSql = Select.selectRenderer(query.lhs, prevContext).render(Some(rhsReferenced))

      val cteColumns = SqlStr.join(
        newExprNaming.collect { case (exprId, name) if rhsReferenced.contains(exprId) => name },
        sql", "
      )

      sql"$withPrefix$cteName ($cteColumns) AS ($lhsSql)$rhsSql"
    }

  }
}
