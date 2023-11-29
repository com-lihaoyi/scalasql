package scalasql.query

import scalasql.dialects.Dialect
import scalasql.renderer.SqlStr.{Renderable, SqlStringSyntax}
import scalasql.{Queryable, TypeMapper}
import scalasql.renderer.{Context, SqlStr}

/**
 * A SQL `VALUES` clause, used to treat a sequence of primitive [[T]]s as
 * a [[Select]] query.
 */
class Values[T: TypeMapper](val ts: Seq[T])(
    implicit val qr: Queryable.Row[Expr[T], T],
    protected val dialect: Dialect
) extends Select.Proxy[Expr[T], T] {
  assert(ts.nonEmpty, "`Values` clause does not support empty sequence")

  protected def selectSimpleFrom() = this.subquery
  val tableRef = new SubqueryRef(this, qr)
  protected def columnName = "column1"
  override protected val expr: Expr[T] = Expr { implicit ctx =>
    val prefix = ctx.fromNaming.get(tableRef) match {
      case Some("") => sql""
      case Some(s) => SqlStr.raw(s) + sql"."
      case None => sql"SCALASQL_MISSING_VALUES."
    }
    prefix + SqlStr.raw(ctx.config.columnNameMapper(columnName))
  }

  override protected def queryWalkExprs(): Seq[(List[String], Expr[_])] = Seq(Nil -> expr)


  override protected def selectRenderer(prevContext: Context): Select.Renderer =
    new Values.Renderer[T](this)(implicitly, prevContext)

  override protected def selectLhsMap(prevContext: Context): Map[Expr.Identity, SqlStr] = {
    Map(Expr.exprIdentity(expr) -> SqlStr.raw(columnName))
  }
}

object Values {
  class Renderer[T: TypeMapper](v: Values[T])(implicit ctx: Context) extends Select.Renderer {
    def wrapRow(t: T) = sql"($t)"
    def render(liveExprs: Option[Set[Expr.Identity]]): SqlStr = {
      val rows = SqlStr.join(v.ts.map(wrapRow), sql", ")
      sql"VALUES $rows"
    }

    def context = ctx
  }
}
