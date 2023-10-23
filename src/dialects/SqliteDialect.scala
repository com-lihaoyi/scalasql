package scalasql.dialects

import scalasql.operations
import scalasql.query.Expr
import scalasql.renderer.SqlStr.SqlStringSyntax

object SqliteDialect extends SqliteDialect {
  class ExprStringOps(v: Expr[String]) extends operations.ExprStringOps(v) {

    def indexOf(x: Expr[String]): Expr[Int] = Expr { implicit ctx => sql"INSTR($v, $x)" }
    def glob(x: Expr[String]): Expr[Int] = Expr { implicit ctx => sql"GLOB($v, $x)" }

    def ltrim(x: Expr[String]): Expr[String] = Expr { implicit ctx => sql"LTRIM($v, $x)" }
    def rtrim(x: Expr[String]): Expr[String] = Expr { implicit ctx => sql"RTRIM($v, $x)" }

  }
}
trait SqliteDialect extends Dialect {
  override implicit def ExprStringOpsConv(v: Expr[String]): SqliteDialect.ExprStringOps =
    new SqliteDialect.ExprStringOps(v)
}
