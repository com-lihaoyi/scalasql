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

class Aggregate[Q, R](toSqlQuery0: Context => (SqlStr, Seq[TypeMapper[_]]), expr: Q)(
    qr: Queryable[Q, R]
) extends Query[R] {

  protected def queryWalkExprs(): Seq[(List[String], Expr[_])] = qr.walk(expr)
  protected def queryIsSingleRow: Boolean = true
  def toSqlQuery(ctx: Context): (SqlStr, Seq[TypeMapper[_]]) = toSqlQuery0(ctx)
  protected def queryValueReader: OptionPickler.Reader[R] = qr.valueReader(expr)
}
