package usql.operations

import usql.Queryable
import usql.query.{Aggregatable, Expr}
import usql.renderer.SqlStr.SqlStringSyntax

class AggNumericOps[V: Numeric](v: Aggregatable[Expr[V]])(implicit qr: Queryable[Expr[V], V]) {
  /** Computes the sum of column values */
  def sum: Expr[V] = v.queryExpr(expr => implicit ctx => usql"SUM($expr)")

  /** Finds the minimum value in a column  */
  def min: Expr[V] = v.queryExpr(expr => implicit ctx => usql"MIN($expr)")

  /** Finds the maximum value in a column  */
  def max: Expr[V] = v.queryExpr(expr => implicit ctx => usql"MAX($expr)")

  /** Computes the average value of a column */
  def avg: Expr[V] = v.queryExpr(expr => implicit ctx => usql"AVG($expr)")
}