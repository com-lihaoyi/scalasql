package scalasql.query

import scalasql.core.{Queryable, SqlStr, Context}

class Aggregate[Q, R](
    toSqlStr0: Context => SqlStr,
    construct0: Queryable.ResultSetIterator => R,
    protected val expr: Q,
    protected val qr: Queryable[Q, R]
) extends Query.DelegateQueryable[Q, R] {

  protected override def queryIsSingleRow: Boolean = true
  private[scalasql] def renderSql(ctx: Context) = toSqlStr0(ctx)

  override protected def queryConstruct(args: Queryable.ResultSetIterator): R = construct0(args)
}
