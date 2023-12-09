package scalasql.operations
import scalasql.core.Db
import scalasql.core.SqlStr.SqlStringSyntax

trait BitwiseFunctionOps[T] extends scalasql.operations.DbNumericOps[T] {
  protected def v: Db[T]
  override def &[V: Numeric](x: Db[V]): Db[T] = Db { implicit ctx => sql"BITAND($v, $x)" }

  override def |[V: Numeric](x: Db[V]): Db[T] = Db { implicit ctx => sql"BITOR($v, $x)" }

  override def unary_~ : Db[T] = Db { implicit ctx => sql"BITNOT($v)" }
}
