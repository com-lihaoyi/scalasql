package scalasql.dialects

import scalasql.query.Expr
import scalasql.renderer.SqlStr.SqlStringSyntax

trait BitwiseFunctionOps[T] extends scalasql.operations.ExprNumericOps[T]{
  def v: Expr[T]
  override def &[V: Numeric](x: Expr[V]): Expr[T] = Expr { implicit ctx => sql"BITAND($v, $x)" }

  override def |[V: Numeric](x: Expr[V]): Expr[T] = Expr { implicit ctx => sql"BITOR($v, $x)" }

  override def unary_~ : Expr[T] = Expr { implicit ctx => sql"BITNOT($v)" }
}
