package scalasql.query

import scalasql.Queryable
import scalasql.renderer.{Context, SqlStr}

/**
 * Something that supports aggregate operations
 */
trait Aggregatable[Q] {
  def expr: Q
  def queryExpr[V](f: Q => Context => SqlStr)(implicit qr: Queryable[Expr[V], V]): Expr[V]
}
