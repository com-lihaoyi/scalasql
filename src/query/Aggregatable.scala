package usql.query

import usql.Queryable
import usql.renderer.{Context, SqlStr}

/**
 * Something that supports aggregate operations
 */
trait Aggregatable[Q] {
  def queryExpr[V](f: Q => Context => SqlStr)
                  (implicit qr: Queryable[Expr[V], V]): Expr[V]
}
