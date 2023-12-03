package scalasql.operations

import scalasql.TypeMapper
import scalasql.query.Expr
import scalasql.renderer.SqlStr.SqlStringSyntax
import scalasql.renderer.{Context, SqlStr}
class CaseWhen[T: TypeMapper](values: Seq[(Expr[Boolean], Expr[T])]) extends Expr[T] {

  def renderToSql0(implicit ctx: Context): SqlStr = {
    val whens = CaseWhen.renderWhens(values)
    sql"CASE $whens END"
  }

  def `else`(other: Expr[T]) = new CaseWhen.Else(values, other)
}
object CaseWhen {
  private def renderWhens[T](values: Seq[(Expr[Boolean], Expr[T])])(implicit ctx: Context) = SqlStr
    .join(values.map { case (when, then_) => sql"WHEN $when THEN $then_" }, sql" ")
  class Else[T: TypeMapper](values: Seq[(Expr[Boolean], Expr[T])], `else`: Expr[T])
      extends Expr[T] {

    def renderToSql0(implicit ctx: Context): SqlStr = {
      val whens = renderWhens(values)
      sql"CASE $whens ELSE ${`else`} END"
    }
  }
}
