package usql

import usql.query.Expr
import usql.renderer.SqlStr.SqlStringSyntax

object PostgresDialect extends PostgresDialect{
  class ExprStringOps(v: Expr[String]) extends operations.ExprStringOps(v){
    def indexOf(x: Expr[String]): Expr[Int] = Expr { implicit ctx => usql"POSITION($x IN $v)" }
  }
}
trait PostgresDialect extends Dialect{
  override implicit def ExprStringOpsConv(v: Expr[String]): PostgresDialect.ExprStringOps = new PostgresDialect.ExprStringOps(v)
}