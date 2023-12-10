package scalasql.operations
import scalasql.core.Expr
import scalasql.core.SqlStr.SqlStringSyntax

trait BitwiseFunctionOps[T] extends scalasql.operations.ExprNumericOps[T] {
  protected def v: Expr[T]
  override def &[V: Numeric](x: Expr[V]): Expr[T] = Expr { implicit ctx => sql"BITAND($v, $x)" }

  override def |[V: Numeric](x: Expr[V]): Expr[T] = Expr { implicit ctx => sql"BITOR($v, $x)" }

  override def unary_~ : Expr[T] = Expr { implicit ctx => sql"BITNOT($v)" }
}
