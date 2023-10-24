package scalasql.dialects

import scalasql.operations
import scalasql.query.Expr
import scalasql.renderer.SqlStr.SqlStringSyntax

object HsqlDbDialect extends HsqlDbDialect {
  class ExprStringOps(val v: Expr[String]) extends operations.ExprStringOps(v) with TrimOps with PadOps{
    def indexOf(x: Expr[String]): Expr[Int] = Expr { implicit ctx => sql"INSTR($v, $x)" }
    def reverse: Expr[String] = Expr { implicit ctx => sql"REVERSE($v)" }
  }

  class ExprNumericOps[T: Numeric](v: Expr[T]) extends operations.ExprNumericOps[T](v){
    override def &[V: Numeric](x: Expr[V]): Expr[T] = Expr { implicit ctx => sql"BITAND($v, $x)" }

    override def |[V: Numeric](x: Expr[V]): Expr[T] = Expr { implicit ctx => sql"BITOR($v, $x)" }

    override def unary_~ : Expr[T] = Expr { implicit ctx => sql"BITNOT($v)" }

    override def unary_- : Expr[T] = Expr { implicit ctx => sql"-($v)" }
  }
}
trait HsqlDbDialect extends Dialect {
  override implicit def ExprStringOpsConv(v: Expr[String]): HsqlDbDialect.ExprStringOps =
    new HsqlDbDialect.ExprStringOps(v)
  override implicit def ExprNumericOpsConv[T: Numeric](v: Expr[T]): HsqlDbDialect.ExprNumericOps[T] =
    new HsqlDbDialect.ExprNumericOps(v)
}
