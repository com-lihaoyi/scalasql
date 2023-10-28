package scalasql.dialects

import scalasql.operations
import scalasql.query.Expr
import scalasql.renderer.SqlStr.SqlStringSyntax

trait PostgresDialect extends Dialect with ReturningDialect with OnConflictOps {
  def defaultQueryableSuffix = ""

  def castParams = false

  override implicit def ExprStringOpsConv(v: Expr[String]): PostgresDialect.ExprStringOps =
    new PostgresDialect.ExprStringOps(v)
}

object PostgresDialect extends PostgresDialect {
  class ExprStringOps(val v: Expr[String])
      extends operations.ExprStringOps(v) with TrimOps with PadOps {
    def indexOf(x: Expr[String]): Expr[Int] = Expr { implicit ctx => sql"POSITION($x IN $v)" }

    def reverse: Expr[String] = Expr { implicit ctx => sql"REVERSE($v)" }
  }
}
