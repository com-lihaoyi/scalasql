package scalasql.query

import scalasql.{MappedType, Queryable}
import scalasql.renderer.{Context, SqlStr}
import scalasql.utils.OptionPickler

/**
 * Something that supports aggregate operations
 */
trait Aggregatable[Q] {
  def expr: Q
  def queryExpr[V: MappedType](f: Q => Context => SqlStr)(implicit
      qr: Queryable[Expr[V], V]
  ): Expr[V]
}

class Aggregate[Q, R](toSqlQuery0: Context => (SqlStr, Seq[MappedType[_]]), expr: Q)(qr: Queryable[
  Q,
  R
]) extends Query[R] {

  def walk(): Seq[(List[String], Expr[_])] = qr.walk(expr)
  def singleRow: Boolean = true
  def toSqlQuery(implicit ctx: Context): (SqlStr, Seq[MappedType[_]]) = toSqlQuery0(ctx)
  def valueReader: OptionPickler.Reader[R] = qr.valueReader(expr)
}
