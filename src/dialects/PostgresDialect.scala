package usql.dialects

import usql.operations
import usql.query.Expr
import usql.renderer.SqlStr.SqlStringSyntax

object PostgresDialect extends PostgresDialect {
  class ExprStringOps(v: Expr[String]) extends operations.ExprStringOps(v) {
    def indexOf(x: Expr[String]): Expr[Int] = Expr { implicit ctx => usql"POSITION($x IN $v)" }

    def ltrim(x: Expr[String]): Expr[String] = Expr { implicit ctx => usql"LTRIM($v, $x)" }
    def rtrim(x: Expr[String]): Expr[String] = Expr { implicit ctx => usql"RTRIM($v, $x)" }
    def rpad(length: Expr[Int], fill: Expr[String]): Expr[String] = Expr { implicit ctx =>
      usql"RPAD($v, $length, $fill)"
    }
    def lpad(length: Expr[Int], fill: Expr[String]): Expr[String] = Expr { implicit ctx =>
      usql"LPAD($v, $length, $fill)"
    }
    def reverse: Expr[String] = Expr { implicit ctx => usql"REVERSE($v)" }
  }
}
trait PostgresDialect extends Dialect {
  override implicit def ExprStringOpsConv(v: Expr[String]): PostgresDialect.ExprStringOps =
    new PostgresDialect.ExprStringOps(v)
}
