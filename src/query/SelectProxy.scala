package scalasql.query

import scalasql.{MappedType, Queryable}
import scalasql.renderer.{Context, SqlStr}

/**
 * A reference that aggregations for usage within [[Select.aggregate]], to allow
 * the caller to perform multiple aggregations within a single query.
 */
class SelectProxy[Q](val expr: Q) extends Aggregatable[Q] {
  def queryExpr[V: MappedType](f: Q => Context => SqlStr)(
      implicit qr: Queryable.Row[Expr[V], V]
  ): Expr[V] = { Expr[V] { implicit c => f(expr)(c) } }
}
