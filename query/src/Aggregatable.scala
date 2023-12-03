package scalasql.query

import scalasql.core.{Queryable, Sql, SqlStr, TypeMapper, Context}

/**
 * Something that supports aggregate operations. Most commonly a [[Select]], but
 * also could be a [[SelectProxy]]
 */
trait Aggregatable[Q] extends WithExpr[Q] {
  def queryExpr[V: TypeMapper](f: Q => Context => SqlStr)(
      implicit qr: Queryable.Row[Sql[V], V]
  ): Sql[V]
}

class Aggregate[Q, R](
    toSqlStr0: Context => SqlStr,
    construct0: Queryable.ResultSetIterator => R,
    expr: Q
)(
    qr: Queryable[Q, R]
) extends Query[R] {

  protected def queryWalkLabels() = qr.walkLabels(expr)
  protected def queryWalkExprs() = qr.walkExprs(expr)
  protected def queryIsSingleRow: Boolean = true
  protected def renderToSql(ctx: Context) = toSqlStr0(ctx)

  override protected def queryConstruct(args: Queryable.ResultSetIterator): R = construct0(args)
}
