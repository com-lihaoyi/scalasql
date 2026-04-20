package scalasql.operations

import scalasql.core.{Aggregatable, Expr, Queryable, TypeMapper}
import scalasql.core.SqlStr.SqlStringSyntax

/**
 * Aggregations that apply to any element type `T`, e.g. COUNT and COUNT(DISTINCT)
 * over an aggregated `Expr[T]` sequence.
 */
class AggAnyOps[T](v: Aggregatable[Expr[T]])(
    implicit tmInt: TypeMapper[Int],
    qrInt: Queryable.Row[Expr[Int], Int]
) {

  /** Counts non-null values */
  def count: Expr[Int] = v.aggregateExpr[Int](expr => implicit ctx => sql"COUNT($expr)")

  /** Counts distinct non-null values */
  def countDistinct: Expr[Int] =
    v.aggregateExpr[Int](expr => implicit ctx => sql"COUNT(DISTINCT $expr)")
}
