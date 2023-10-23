package scalasql.dialects

import scalasql.operations
import scalasql.query.Expr
import scalasql.renderer.SqlStr.SqlStringSyntax

object H2Dialect extends H2Dialect {
  class ExprStringOps(v: Expr[String]) extends operations.ExprStringOps(v) {
    def indexOf(x: Expr[String]): Expr[Int] = Expr { implicit ctx => sql"INSTR($v, $x)" }
    def ltrim(x: Expr[String]): Expr[String] = Expr { implicit ctx => sql"LTRIM($v, $x)" }
    def rtrim(x: Expr[String]): Expr[String] = Expr { implicit ctx => sql"RTRIM($v, $x)" }
    def rpad(length: Expr[Int], fill: Expr[String]): Expr[String] = Expr { implicit ctx =>
      sql"RPAD($v, $length, $fill)"
    }

    def lpad(length: Expr[Int], fill: Expr[String]): Expr[String] = Expr { implicit ctx =>
      sql"LPAD($v, $length, $fill)"
    }
  }

  class ExprNumericOps[T: Numeric](v: Expr[T]) extends operations.ExprNumericOps[T](v){
    override def &[V: Numeric](x: Expr[V]): Expr[T] = Expr { implicit ctx => sql"BITAND($v, $x)" }

    override def |[V: Numeric](x: Expr[V]): Expr[T] = Expr { implicit ctx => sql"BITOR($v, $x)" }

    override def unary_~ : Expr[T] = Expr { implicit ctx => sql"BITNOT($v)" }

    override def unary_- : Expr[T] = Expr { implicit ctx => sql"-($v)" }
  }
}
trait H2Dialect extends Dialect {
  override implicit def ExprStringOpsConv(v: Expr[String]): HsqlDbDialect.ExprStringOps =
    new HsqlDbDialect.ExprStringOps(v)
  override implicit def ExprNumericOpsConv[T: Numeric](v: Expr[T]): HsqlDbDialect.ExprNumericOps[T] =
    new HsqlDbDialect.ExprNumericOps(v)
}
