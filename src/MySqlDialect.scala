package usql

import usql.query.Expr
import usql.renderer.SqlStr.SqlStringSyntax

object MySqlDialect extends MySqlDialect{
  class ExprStringOps(v: Expr[String]) extends operations.ExprStringOps(v){
    def indexOf(x: Expr[String]): Expr[Int] = Expr { implicit ctx => usql"POSITION($x IN $v)" }
  }
}
trait MySqlDialect extends Dialect{
  override implicit def ExprStringOpsConv(v: Expr[String]): MySqlDialect.ExprStringOps = new MySqlDialect.ExprStringOps(v)
}