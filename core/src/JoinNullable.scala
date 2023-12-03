package scalasql.core

import scalasql.core.{Queryable, Sql, TypeMapper}
import scalasql.core.Context
import scalasql.core.SqlStr.SqlStringSyntax

/**
 * Represents a set of nullable columns that come from a `LEFT`/`RIGHT`/`OUTER` `JOIN`
 * clause.
 */
trait JoinNullable[Q] {
  def get: Q
  def isEmpty[T](f: Q => Sql[T])(implicit qr: Queryable[Q, _]): Sql[Boolean]
  def nonEmpty[T](f: Q => Sql[T])(implicit qr: Queryable[Q, _]): Sql[Boolean]
  def map[V](f: Q => V): JoinNullable[V]

}
object JoinNullable {
  implicit def toExpr[T](n: JoinNullable[Sql[T]])(implicit mt: TypeMapper[T]): Sql[Option[T]] =
    Sql { implicit ctx => sql"${n.get}" }

  def apply[Q](t: Q): JoinNullable[Q] = new JoinNullable[Q] {
    def get: Q = t
    def isEmpty[T](f: Q => Sql[T])(implicit qr: Queryable[Q, _]): Sql[Boolean] = Sql {
      implicit ctx =>
        sql"(${f(t)} IS NULL)"
    }
    def nonEmpty[T](f: Q => Sql[T])(implicit qr: Queryable[Q, _]): Sql[Boolean] = Sql {
      implicit ctx =>
        sql"(${f(t)} IS NOT NULL)"
    }
    def map[V](f: Q => V) = JoinNullable(f(t))
  }

}
