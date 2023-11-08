package scalasql.query

import scalasql.Queryable
import scalasql.renderer.SqlStr
import scalasql.renderer.SqlStr.SqlStringSyntax


trait Nullable[Q]{
  def get: Q
  def isEmpty(implicit qr: Queryable[Q, _]): Expr[Boolean]
  def map[V](f: Q => V): Nullable[V]
}
object Nullable{
  def apply[Q](t: Q): Nullable[Q] = new Nullable[Q] {
    def get: Q = t
    def isEmpty(implicit qr: Queryable[Q, _]): Expr[Boolean] = Expr{implicit ctx =>

      SqlStr.join(qr.walk(t).map{case (s, e) => sql"$e IS NULL"}, sql" AND ")
    }
    def map[V](f: Q => V) = Nullable(f(t))
  }
}