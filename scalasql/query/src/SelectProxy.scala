package scalasql.query

import scalasql.core.{Aggregatable, Context, Queryable, Db, SqlStr, TypeMapper}

/**
 * A reference that aggregations for usage within [[Select.aggregate]], to allow
 * the caller to perform multiple aggregations within a single query.
 */
class SelectProxy[Q](val expr: Q) extends Aggregatable[Q] {
  def queryExpr[V: TypeMapper](f: Q => Context => SqlStr)(
      implicit qr: Queryable.Row[Db[V], V]
  ): Db[V] = { Db[V] { implicit c => f(expr)(c) } }
}
