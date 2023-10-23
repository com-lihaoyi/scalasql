package scalasql.operations

import scalasql.Queryable
import scalasql.query.{Aggregatable, Expr}
import scalasql.renderer.SqlStr.SqlStringSyntax

class AggNumericOps[V: Numeric](v: Aggregatable[Expr[V]])(implicit qr: Queryable[Expr[V], V]) {

  /** Computes the sum of column values */
  def sum: Expr[V] = v.queryExpr(expr => implicit ctx => sql"SUM($expr)")

  /** Finds the minimum value in a column */
  def min: Expr[V] = v.queryExpr(expr => implicit ctx => sql"MIN($expr)")

  /** Finds the maximum value in a column */
  def max: Expr[V] = v.queryExpr(expr => implicit ctx => sql"MAX($expr)")

  /** Computes the average value of a column */
  def avg: Expr[V] = v.queryExpr(expr => implicit ctx => sql"AVG($expr)")
}
