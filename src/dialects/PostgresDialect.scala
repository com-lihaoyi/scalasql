package scalasql.dialects

import scalasql.operations
import scalasql.query.Expr
import scalasql.renderer.SqlStr.SqlStringSyntax

object PostgresDialect extends PostgresDialect {
  class ExprStringOps(v: Expr[String]) extends operations.ExprStringOps(v) {
    def indexOf(x: Expr[String]): Expr[Int] = Expr { implicit ctx => sql"POSITION($x IN $v)" }

    def ltrim(x: Expr[String]): Expr[String] = Expr { implicit ctx => sql"LTRIM($v, $x)" }
    def rtrim(x: Expr[String]): Expr[String] = Expr { implicit ctx => sql"RTRIM($v, $x)" }
    def rpad(length: Expr[Int], fill: Expr[String]): Expr[String] = Expr { implicit ctx =>
      sql"RPAD($v, $length, $fill)"
    }
    def lpad(length: Expr[Int], fill: Expr[String]): Expr[String] = Expr { implicit ctx =>
      sql"LPAD($v, $length, $fill)"
    }
    def reverse: Expr[String] = Expr { implicit ctx => sql"REVERSE($v)" }
  }
}
trait PostgresDialect extends Dialect {
  override implicit def ExprStringOpsConv(v: Expr[String]): PostgresDialect.ExprStringOps =
    new PostgresDialect.ExprStringOps(v)
}
