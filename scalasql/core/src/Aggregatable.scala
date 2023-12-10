package scalasql.core

/**
 * Something that supports aggregate operations. Most commonly a [[Select]], but
 * also could be a [[SelectProxy]]
 */
trait Aggregatable[Q] extends WithSqlExpr[Q] {
  def aggregateExpr[V: TypeMapper](f: Q => Context => SqlStr)(
      implicit qr: Queryable.Row[Db[V], V]
  ): Db[V]
}
