package scalasql.query

import scalasql.core.{Context, DialectTypeMappers, Expr, LiveSqlExprs, Queryable, SqlStr, TypeMapper, WithSqlExpr}
import scalasql.core.SqlStr.{Renderable, SqlStringSyntax}

/**
 * A SQL `VALUES` clause, used to treat a sequence of primitive [[T]]s as
 * a [[Select]] query.
 */
class Values[Q, R](val ts: Seq[R])(
    implicit val qr: Queryable.Row[Q, R],
    protected val dialect: DialectTypeMappers
) extends Select.Proxy[Q, R]
    with Query.DelegateQueryable[Q, Seq[R]] {
  assert(ts.nonEmpty, "`Values` clause does not support empty sequence")

  protected def selectToSimpleSelect() = this.subquery
  val tableRef = new SubqueryRef(this)
  protected def columnName(n: Int) = s"column${n + 1}"

  protected override val expr: Q = qr.deconstruct(ts.head)

  override protected def selectRenderer(prevContext: Context): SubqueryRef.Wrapped.Renderer =
    new Values.Renderer(this)(implicitly, prevContext)

  override protected def selectExprAliases(prevContext: Context): Map[Expr.Identity, SqlStr] = {
    qr.walkExprs(expr)
      .zipWithIndex
      .map { case (e, i) => (Expr.identity(e), SqlStr.raw(columnName(i))) }
      .toMap
  }

}

object Values {
  class Renderer[Q, R](v: Values[Q, R])(implicit qr: Queryable.Row[Q, R], ctx: Context)
      extends SubqueryRef.Wrapped.Renderer {
    def wrapRow(t: R): SqlStr = sql"(" + SqlStr.join(
      qr.walkExprs(qr.deconstruct(t)).map(i => sql"$i"),
      SqlStr.commaSep
    ) + sql")"
    def render(liveExprs: LiveSqlExprs): SqlStr = {
      val rows = SqlStr.join(v.ts.map(wrapRow), SqlStr.commaSep)
      sql"VALUES $rows"
    }

    def context = ctx
  }
}
