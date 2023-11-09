package scalasql.query

import scalasql.{MappedType, Queryable}
import scalasql.renderer.{Context, SqlStr}
import scalasql.renderer.SqlStr.SqlStringSyntax

/**
 * Represents a set of nullable columns that come from a `LEFT`/`RIGHT`/`OUTER` `JOIN`
 * clause.
 */
trait Nullable[Q] {
  def get: Q
  def isEmpty(implicit qr: Queryable[Q, _]): Expr[Boolean]
  def map[V](f: Q => V): Nullable[V]

}
object Nullable {
  implicit class NullableExpr[T](n: Nullable[Expr[T]]){
    def toExpr(implicit mt: MappedType[T]): Expr[Option[T]] = Expr { implicit ctx =>
      sql"${n.get}"
    }
  }
  def apply[Q](t: Q): Nullable[Q] = new Nullable[Q] {
    def get: Q = t
    def isEmpty(implicit qr: Queryable[Q, _]): Expr[Boolean] = Expr { implicit ctx =>
      SqlStr.join(qr.walk(t).map { case (s, e) => sql"$e IS NULL" }, sql" AND ")
    }
    def map[V](f: Q => V) = Nullable(f(t))
  }
}
