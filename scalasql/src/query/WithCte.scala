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
)(implicit val qr: Queryable.Row[Q, R])
    extends Select[Q, R] {


  protected def expr = WithExpr.get(Joinable.joinableSelect(rhs))

  protected override def joinableSelect = this

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

  def mapAggregate[Q2, R2](f: (Q, SelectProxy[Q]) => Q2)(implicit qr: Queryable.Row[Q2, R2]): Select[Q2, R2] = {
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
    new WithCte.Renderer(this, prevContext)
  protected override def queryTypeMappers() = qr.toTypeMappers(expr)

  protected def selectLhsMap(prevContext: Context): Map[Expr.Identity, SqlStr] = {
    Select.selectLhsMap(lhs, prevContext)
  }
}

object WithCte {

  class Renderer[Q, R](query: WithCte[Q, R], prevContext: Context) extends Select.Renderer {

    def render(liveExprs: Option[Set[Expr.Identity]]) = {
      val lhsRenderer = Select.selectRenderer(query.lhs, prevContext)
      val lhsSql = lhsRenderer.render(liveExprs)

      val walked = query.lhs.qr.asInstanceOf[Queryable[Any, Any]].walk(WithExpr.get(query.lhs))
      val rhsSql = Select.selectRenderer(
        query.rhs,
        prevContext.withExprNaming(
          prevContext.exprNaming ++
            walked.map{case (tokens, expr) =>
              (Expr.exprIdentity(expr), SqlStr.raw((prevContext.config.columnLabelPrefix +: tokens.map(prevContext.config.columnNameMapper)).mkString(prevContext.config.columnLabelDelimiter)))
            }
        )
      ).render(liveExprs)
      val cteColumns = SqlStr.join(
        FlatJson.flatten(walked, prevContext).map(t => SqlStr.raw(t._1)),
        sql", "
      )

      sql"WITH ${SqlStr.raw(query.lhsSubQuery.name)}($cteColumns) AS ($lhsSql) $rhsSql"
    }

  }
}
