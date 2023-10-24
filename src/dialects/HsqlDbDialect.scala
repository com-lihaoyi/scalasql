package scalasql.dialects

import scalasql.operations
import scalasql.query.Expr
import scalasql.renderer.SqlStr.SqlStringSyntax

object HsqlDbDialect extends HsqlDbDialect {
  class ExprStringOps(val v: Expr[String]) extends operations.ExprStringOps(v) with TrimOps with PadOps{
    def indexOf(x: Expr[String]): Expr[Int] = Expr { implicit ctx => sql"INSTR($v, $x)" }
    def reverse: Expr[String] = Expr { implicit ctx => sql"REVERSE($v)" }
  }

  class ExprNumericOps[T: Numeric](val v: Expr[T]) extends operations.ExprNumericOps[T](v) with BitwiseFunctionOps[T]
}
trait HsqlDbDialect extends Dialect {
  override implicit def ExprStringOpsConv(v: Expr[String]): HsqlDbDialect.ExprStringOps =
    new HsqlDbDialect.ExprStringOps(v)
  override implicit def ExprNumericOpsConv[T: Numeric](v: Expr[T]): HsqlDbDialect.ExprNumericOps[T] =
    new HsqlDbDialect.ExprNumericOps(v)
}
