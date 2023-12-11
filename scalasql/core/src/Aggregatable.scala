package scalasql.core

/**
 * Something that supports aggregate operations. Most commonly a [[Select]], but
 * also could be a [[Aggregatable.Proxy]]
 */
trait Aggregatable[Q] extends WithSqlExpr[Q] {
  def aggregateExpr[V: TypeMapper](f: Q => Context => SqlStr)(
      implicit qr: Queryable.Row[Expr[V], V]
  ): Expr[V]
}

object Aggregatable {

  /**
   * A reference that aggregations for usage within [[Select.aggregate]], to allow
   * the caller to perform multiple aggregations within a single query.
   */
  class Proxy[Q](val expr: Q) extends Aggregatable[Q] {
    def aggregateExpr[V: TypeMapper](f: Q => Context => SqlStr)(
        implicit qr: Queryable.Row[Expr[V], V]
    ): Expr[V] = { Expr[V] { implicit c => f(expr)(c) } }
  }

}
