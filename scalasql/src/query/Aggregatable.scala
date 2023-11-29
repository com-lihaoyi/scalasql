package scalasql.query

import scalasql.{Queryable, ResultSetIterator, TypeMapper}
import scalasql.renderer.{Context, SqlStr}

/**
 * Something that supports aggregate operations. Most commonly a [[Select]], but
 * also could be a [[SelectProxy]]
 */
trait Aggregatable[Q] extends WithExpr[Q] {
  def queryExpr[V: TypeMapper](f: Q => Context => SqlStr)(
      implicit qr: Queryable.Row[Expr[V], V]
  ): Expr[V]
}

class Aggregate[Q, R](toSqlStr0: Context => SqlStr, construct0: ResultSetIterator => R, expr: Q)(
    qr: Queryable[Q, R]
) extends Query[R] {

  protected def queryWalkExprs(): Seq[(List[String], Expr[_])] = qr.walk(expr)
  protected def queryIsSingleRow: Boolean = true
  protected def renderToSql(ctx: Context) = toSqlStr0(ctx)

  override protected def queryConstruct(args: ResultSetIterator): R = construct0(args)
}
