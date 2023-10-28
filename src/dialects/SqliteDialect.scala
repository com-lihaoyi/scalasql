package scalasql.dialects

import scalasql.operations
import scalasql.query.Expr
import scalasql.renderer.SqlStr.SqlStringSyntax

object SqliteDialect extends SqliteDialect {
  class ExprStringOps(val v: Expr[String]) extends operations.ExprStringOps(v) with TrimOps {
    def indexOf(x: Expr[String]): Expr[Int] = Expr { implicit ctx => sql"INSTR($v, $x)" }
    def glob(x: Expr[String]): Expr[Int] = Expr { implicit ctx => sql"GLOB($v, $x)" }
  }
}
trait SqliteDialect extends Dialect with ReturningDialect with OnConflictOps {
  override implicit def ExprStringOpsConv(v: Expr[String]): SqliteDialect.ExprStringOps =
    new SqliteDialect.ExprStringOps(v)
}
