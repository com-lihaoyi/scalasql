package scalasql.query

import scalasql.core.{Queryable, Sql, SqlStr, TypeMapper, Context}


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
