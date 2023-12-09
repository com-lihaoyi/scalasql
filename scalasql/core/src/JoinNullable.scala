package scalasql.core

import scalasql.core.{Queryable, Db, TypeMapper}
import scalasql.core.Context
import scalasql.core.SqlStr.SqlStringSyntax

/**
 * Represents a set of nullable columns that come from a `LEFT`/`RIGHT`/`OUTER` `JOIN`
 * clause.
 */
trait JoinNullable[Q] {
  def get: Q
  def isEmpty[T](f: Q => Db[T])(implicit qr: Queryable[Q, _]): Db[Boolean]
  def nonEmpty[T](f: Q => Db[T])(implicit qr: Queryable[Q, _]): Db[Boolean]
  def map[V](f: Q => V): JoinNullable[V]

}
object JoinNullable {
  implicit def toExpr[T](n: JoinNullable[Db[T]])(implicit mt: TypeMapper[T]): Db[Option[T]] =
    Db { implicit ctx => sql"${n.get}" }

  def apply[Q](t: Q): JoinNullable[Q] = new JoinNullable[Q] {
    def get: Q = t
    def isEmpty[T](f: Q => Db[T])(implicit qr: Queryable[Q, _]): Db[Boolean] = Db { implicit ctx =>
      sql"(${f(t)} IS NULL)"
    }
    def nonEmpty[T](f: Q => Db[T])(implicit qr: Queryable[Q, _]): Db[Boolean] = Db { implicit ctx =>
      sql"(${f(t)} IS NOT NULL)"
    }
    def map[V](f: Q => V) = JoinNullable(f(t))
  }

}
