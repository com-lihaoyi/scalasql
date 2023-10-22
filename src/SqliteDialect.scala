package usql

import usql.query.Expr
import usql.renderer.SqlStr.SqlStringSyntax

object SqliteDialect extends SqliteDialect{
  class ExprStringOps(v: Expr[String]) extends operations.ExprStringOps(v){

    def indexOf(x: Expr[String]): Expr[Int] = Expr { implicit ctx => usql"INSTR($v, $x)" }
  }
}
trait SqliteDialect extends Dialect{
  override implicit def ExprStringOpsConv(v: Expr[String]): SqliteDialect.ExprStringOps = new SqliteDialect.ExprStringOps(v)
}
