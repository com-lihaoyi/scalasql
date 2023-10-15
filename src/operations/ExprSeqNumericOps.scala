package usql.operations

import usql.Queryable
import usql.query.{Aggregatable, Expr}
import usql.renderer.SqlStr.SqlStringSyntax

class ExprSeqNumericOps[V: Numeric](v: Aggregatable[Expr[V]])(implicit qr: Queryable[Expr[V], V]) {
  /** Computes the sum of column values */
  def sum: Expr[V] = v.queryExpr(implicit ctx => usql"SUM(${v.expr})")

  /** Finds the minimum value in a column  */
  def min: Expr[V] = v.queryExpr(implicit ctx => usql"MIN(${v.expr})")

  /** Finds the maximum value in a column  */
  def max: Expr[V] = v.queryExpr(implicit ctx => usql"MAX(${v.expr})")

  /** Computes the average value of a column */
  def avg: Expr[V] = v.queryExpr(implicit ctx => usql"AVG(${v.expr})")
}