package scalasql.query

import scalasql.core.{Context, DialectTypeMappers, LiveSqlExprs, Queryable, Db, SqlStr, TypeMapper}
import scalasql.core.SqlStr.{Renderable, SqlStringSyntax}

/**
 * A SQL `VALUES` clause, used to treat a sequence of primitive [[T]]s as
 * a [[Select]] query.
 */
class Values[Q, R](val ts: Seq[R])(
    implicit val qr: Queryable.Row[Q, R],
    protected val dialect: DialectTypeMappers
) extends Select.Proxy[Q, R] {
  assert(ts.nonEmpty, "`Values` clause does not support empty sequence")

  protected def selectToSimpleSelect() = this.subquery
  val tableRef = new SubqueryRef(this, qr)
  protected def columnName(n: Int) = s"column${n + 1}"

  override val expr: Q = qr.deconstruct(ts.head)

  override protected def queryWalkLabels() = qr.walkExprs(expr).indices.map(i => List(i.toString))

  override protected def queryWalkExprs() = qr.walkExprs(expr)

  override protected def selectRenderer(prevContext: Context): SelectBase.Renderer =
    new Values.Renderer(this)(implicitly, prevContext)

  override protected def selectLhsMap(prevContext: Context): Map[Db.Identity, SqlStr] = {
    qr.walkExprs(expr)
      .zipWithIndex
      .map { case (e, i) => (Db.identity(e), SqlStr.raw(columnName(i))) }
      .toMap
  }
}

object Values {
  class Renderer[Q, R](v: Values[Q, R])(implicit qr: Queryable.Row[Q, R], ctx: Context)
      extends SelectBase.Renderer {
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
