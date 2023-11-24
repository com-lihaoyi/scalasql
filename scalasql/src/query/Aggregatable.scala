package scalasql.query

import scalasql.{TypeMapper, Queryable}
import scalasql.renderer.{Context, SqlStr}
import scalasql.utils.OptionPickler

/**
 * Something that supports aggregate operations. Most commonly a [[Select]], but
 * also could be a [[SelectProxy]]
 */
trait Aggregatable[Q] extends WithExpr[Q] {
  def queryExpr[V: TypeMapper](f: Q => Context => SqlStr)(
      implicit qr: Queryable.Row[Expr[V], V]
  ): Expr[V]
}

class Aggregate[Q, R](toSqlStr0: Context => SqlStr, toTypeMappers: Seq[TypeMapper[_]], expr: Q)(
    qr: Queryable[Q, R]
) extends Query[R] {

  protected def queryWalkExprs(): Seq[(List[String], Expr[_])] = qr.walk(expr)
  protected def queryIsSingleRow: Boolean = true
  protected def renderToSql(ctx: Context) = toSqlStr0(ctx)
  protected def queryTypeMappers() = toTypeMappers
  protected def queryValueReader: OptionPickler.Reader[R] = qr.valueReader(expr)
}
