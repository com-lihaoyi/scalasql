package scalasql.operations

import scalasql.MappedType
import scalasql.query.Expr
import scalasql.renderer.SqlStr.SqlStringSyntax
import scalasql.renderer.{Context, SqlStr}
class CaseWhen[T: MappedType](values: Seq[(Expr[Boolean], Expr[T])]) extends Expr[T] {
  def mappedType = implicitly

  def toSqlExpr0(implicit ctx: Context): SqlStr = {
    val whens = SqlStr
      .join(values.map { case (when, then) => sql"WHEN $when THEN $then" }, sql" ")

    sql"CASE $whens END"
  }

  def `else`(other: Expr[T]) = new CaseWhen.Else(values, other)
}
object CaseWhen {

  class Else[T: MappedType](values: Seq[(Expr[Boolean], Expr[T])], `else`: Expr[T]) extends Expr[T] {
    def mappedType = implicitly

    def toSqlExpr0(implicit ctx: Context): SqlStr = {
      val whens = SqlStr
        .join(values.map { case (when, then) => sql"WHEN $when THEN $then" }, sql" ")

      sql"CASE $whens ELSE ${`else`} END"
    }
  }
}