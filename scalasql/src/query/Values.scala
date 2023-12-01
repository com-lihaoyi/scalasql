package scalasql.query

import scalasql.dialects.Dialect
import scalasql.renderer.SqlStr.{Renderable, SqlStringSyntax}
import scalasql.{Queryable, TypeMapper}
import scalasql.renderer.{Context, SqlStr}

/**
 * A SQL `VALUES` clause, used to treat a sequence of primitive [[T]]s as
 * a [[Select]] query.
 */
class Values[Q, R](val ts: Seq[R])(
    implicit val qr: Queryable.Row[Q, R],
    protected val dialect: Dialect
) extends Select.Proxy[Q, R] {
  assert(ts.nonEmpty, "`Values` clause does not support empty sequence")

  protected def selectSimpleFrom() = this.subquery
  val tableRef = new SubqueryRef(this, qr)
  protected def columnName(n: Int) = s"column${n + 1}"

  override val expr: Q = qr.deconstruct(ts.head)

  override protected def queryWalkExprs(): Seq[(List[String], Expr[_])] = {
    qr.walkExprs(expr).zipWithIndex.map { case (e, i) => List(i.toString) -> (e: Expr[_]) }
  }

  override protected def selectRenderer(prevContext: Context): Select.Renderer =
    new Values.Renderer(this)(implicitly, prevContext)

  override protected def selectLhsMap(prevContext: Context): Map[Expr.Identity, SqlStr] = {
    qr.walkExprs(expr)
      .zipWithIndex
      .map { case (e, i) => (Expr.exprIdentity(e), SqlStr.raw(columnName(i))) }
      .toMap
  }
}

object Values {
  class Renderer[Q, R](v: Values[Q, R])(implicit qr: Queryable.Row[Q, R], ctx: Context)
      extends Select.Renderer {
    def wrapRow(t: R): SqlStr = sql"(" + SqlStr.join(
      qr.walkExprs(qr.deconstruct(t)).map(i => sql"$i"),
      SqlStr.commaSep
    ) + sql")"
    def render(liveExprs: Option[Set[Expr.Identity]]): SqlStr = {
      val rows = SqlStr.join(v.ts.map(wrapRow), SqlStr.commaSep)
      sql"VALUES $rows"
    }

    def context = ctx
  }
}
