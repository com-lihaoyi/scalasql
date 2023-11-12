package scalasql.query

import scalasql.{MappedType, Queryable}
import scalasql.renderer.{Context, SqlStr}
import scalasql.renderer.SqlStr.SqlStringSyntax

/**
 * Represents a set of nullable columns that come from a `LEFT`/`RIGHT`/`OUTER` `JOIN`
 * clause.
 */
trait JoinNullable[Q] {
  def get: Q
  def isEmpty[T](f: Q => Expr[T])(implicit qr: Queryable[Q, _]): Expr[Boolean]
  def map[V](f: Q => V): JoinNullable[V]

}
object JoinNullable {
  implicit def toExpr[T](n: JoinNullable[Expr[T]])(implicit mt: MappedType[T]): Expr[Option[T]] =
    Expr { implicit ctx => sql"${n.get}" }

  def apply[Q](t: Q): JoinNullable[Q] = new JoinNullable[Q] {
    def get: Q = t
    def isEmpty[T](f: Q => Expr[T])(implicit qr: Queryable[Q, _]): Expr[Boolean] = Expr {
      implicit ctx =>
        sql"${f(t)} IS NULL"
    }
    def map[V](f: Q => V) = JoinNullable(f(t))
  }
}
