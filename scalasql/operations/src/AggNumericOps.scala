package scalasql.operations

import scalasql.core.{Queryable, Sql, TypeMapper}
import scalasql.core.Aggregatable
import scalasql.core.SqlStr.SqlStringSyntax

class AggNumericOps[V: Numeric: TypeMapper](v: Aggregatable[Sql[V]])(
    implicit qr: Queryable.Row[Sql[V], V]
) {

  /** Computes the sum of column values */
  def sum: Sql[V] = v.queryExpr(expr => implicit ctx => sql"SUM($expr)")

  /** Finds the minimum value in a column */
  def min: Sql[V] = v.queryExpr(expr => implicit ctx => sql"MIN($expr)")

  /** Finds the maximum value in a column */
  def max: Sql[V] = v.queryExpr(expr => implicit ctx => sql"MAX($expr)")

  /** Computes the average value of a column */
  def avg: Sql[V] = v.queryExpr(expr => implicit ctx => sql"AVG($expr)")
}
