package scalasql.operations

import scalasql.core.TypeMapper
import scalasql.core.Sql
import scalasql.core.SqlStr
import scalasql.core.SqlStr.SqlStringSyntax
import scalasql.core.Context
class CaseWhen[T: TypeMapper](values: Seq[(Sql[Boolean], Sql[T])]) extends Sql[T] {

  def renderToSql0(implicit ctx: Context): SqlStr = {
    val whens = CaseWhen.renderWhens(values)
    sql"CASE $whens END"
  }

  def `else`(other: Sql[T]) = new CaseWhen.Else(values, other)
}
object CaseWhen {
  private def renderWhens[T](values: Seq[(Sql[Boolean], Sql[T])])(implicit ctx: Context) = SqlStr
    .join(values.map { case (when, then_) => sql"WHEN $when THEN $then_" }, sql" ")
  class Else[T: TypeMapper](values: Seq[(Sql[Boolean], Sql[T])], `else`: Sql[T]) extends Sql[T] {

    def renderToSql0(implicit ctx: Context): SqlStr = {
      val whens = renderWhens(values)
      sql"CASE $whens ELSE ${`else`} END"
    }
  }
}
