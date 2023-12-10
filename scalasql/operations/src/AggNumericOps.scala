package scalasql.operations

import scalasql.core.{Queryable, Expr, TypeMapper}
import scalasql.core.Aggregatable
import scalasql.core.SqlStr.SqlStringSyntax

class AggNumericOps[V: Numeric: TypeMapper](v: Aggregatable[Expr[V]])(
    implicit qr: Queryable.Row[Expr[V], V]
) {

  /** Computes the sum of column values */
  def sum: Expr[V] = v.aggregateExpr(expr => implicit ctx => sql"SUM($expr)")

  /** Finds the minimum value in a column */
  def min: Expr[V] = v.aggregateExpr(expr => implicit ctx => sql"MIN($expr)")

  /** Finds the maximum value in a column */
  def max: Expr[V] = v.aggregateExpr(expr => implicit ctx => sql"MAX($expr)")

  /** Computes the average value of a column */
  def avg: Expr[V] = v.aggregateExpr(expr => implicit ctx => sql"AVG($expr)")
}
