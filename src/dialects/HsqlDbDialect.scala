package usql.dialects

import usql.operations
import usql.query.Expr
import usql.renderer.SqlStr.SqlStringSyntax

object HsqlDbDialect extends HsqlDbDialect {
  class ExprStringOps(v: Expr[String]) extends operations.ExprStringOps(v) {
    def indexOf(x: Expr[String]): Expr[Int] = Expr { implicit ctx => usql"INSTR($v, $x)" }
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

  class ExprNumericOps[T: Numeric](v: Expr[T]) extends operations.ExprNumericOps[T](v){
    override def &[V: Numeric](x: Expr[V]): Expr[T] = Expr { implicit ctx => usql"BITAND($v, $x)" }

    override def |[V: Numeric](x: Expr[V]): Expr[T] = Expr { implicit ctx => usql"BITOR($v, $x)" }

    override def unary_~ : Expr[T] = Expr { implicit ctx => usql"BITNOT($v)" }

    override def unary_- : Expr[T] = Expr { implicit ctx => usql"-($v)" }
  }
}
trait HsqlDbDialect extends Dialect {
  override implicit def ExprStringOpsConv(v: Expr[String]): HsqlDbDialect.ExprStringOps =
    new HsqlDbDialect.ExprStringOps(v)
  override implicit def ExprNumericOpsConv[T: Numeric](v: Expr[T]): HsqlDbDialect.ExprNumericOps[T] =
    new HsqlDbDialect.ExprNumericOps(v)
}
