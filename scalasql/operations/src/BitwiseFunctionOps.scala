package scalasql.operations
import scalasql.core.Sql
import scalasql.core.SqlStr.SqlStringSyntax

trait BitwiseFunctionOps[T] extends scalasql.operations.ExprNumericOps[T] {
  protected def v: Sql[T]
  override def &[V: Numeric](x: Sql[V]): Sql[T] = Sql { implicit ctx => sql"BITAND($v, $x)" }

  override def |[V: Numeric](x: Sql[V]): Sql[T] = Sql { implicit ctx => sql"BITOR($v, $x)" }

  override def unary_~ : Sql[T] = Sql { implicit ctx => sql"BITNOT($v)" }
}
