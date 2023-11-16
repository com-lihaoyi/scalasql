package scalasql.query

import scalasql.renderer.SqlStr.{Renderable, SqlStringSyntax}
import scalasql.{Queryable, TypeMapper}
import scalasql.renderer.{Context, SqlStr}

object Values{
  def apply[T: TypeMapper](ts: Seq[T]) = new Values(ts)
}

class Values[T: TypeMapper](ts: Seq[T]) extends Renderable with Aggregatable[Expr[T]]{
  def queryExpr[V: TypeMapper](f: Expr[T] => Context => SqlStr)
                              (implicit qr: Queryable.Row[Expr[V], V]): Expr[V] = Expr{
    implicit ctx => sql"SELECT ${f(expr)(ctx)} AS res FROM ${renderToSql(ctx)}".withCompleteQuery(true)
  }

  protected def expr: Expr[T] = Expr{implicit ctx => sql"column1" }

  def contains(other: Expr[_]): Expr[Boolean] = Expr { implicit ctx => sql"($other in $this)" }

  protected def renderToSql(ctx: Context): SqlStr = {
    val rows = SqlStr.join(ts.map(t => sql"($t)"), sql", ")
    sql"(VALUES $rows)"
  }
}
