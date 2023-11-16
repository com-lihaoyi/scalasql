package scalasql.query

import scalasql.renderer.SqlStr.{Renderable, SqlStringSyntax}
import scalasql.{Queryable, TypeMapper}
import scalasql.renderer.{Context, SqlStr}
import scalasql.utils.OptionPickler


class Values[T: TypeMapper](ts: Seq[T]) extends Renderable with Aggregatable[Expr[T]] with Query[Seq[T]]{
  def queryExpr[V: TypeMapper](f: Expr[T] => Context => SqlStr)
                              (implicit qr: Queryable.Row[Expr[V], V]): Expr[V] = Expr{
    implicit ctx => sql"SELECT ${f(expr)(ctx)} AS res FROM (${renderToSql(ctx)}) v".withCompleteQuery(true)
  }

  protected def expr: Expr[T] = Expr{implicit ctx => sql"column1" }

  def contains(other: Expr[_]): Expr[Boolean] = Expr { implicit ctx => sql"($other in ($this))" }

  protected def renderToSql(ctx: Context): SqlStr = {
    val rows = SqlStr.join(ts.map(t => sql"($t)"), sql", ")
    sql"VALUES $rows"
  }

  protected def queryWalkExprs(): Seq[(List[String], Expr[_])] = Seq(Nil -> expr)

  protected def queryIsSingleRow: Boolean = false

  protected def queryValueReader: OptionPickler.Reader[Seq[T]] = implicitly[OptionPickler.Reader[Seq[T]]]

  override protected def queryTypeMappers(): Seq[TypeMapper[_]] = Seq(implicitly[TypeMapper[T]])
}
