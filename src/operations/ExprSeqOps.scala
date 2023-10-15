package usql.operations

import usql.Queryable
import usql.query.{Aggregatable, Expr}
import usql.renderer.SqlStr.SqlStringSyntax

class ExprSeqOps[T](v: Aggregatable[T])(implicit qr: Queryable[T, _]) {
  /** Counts the rows */
  def size: Expr[Int] = v.queryExpr(implicit ctx => usql"COUNT(1)")

  /** Computes the sum of column values */
  def sumBy[V: Numeric](f: T => Expr[V])(implicit qr: Queryable[Expr[V], V]): Expr[V] = v.queryExpr(implicit ctx => usql"SUM(${f(v.expr)})")

  /** Finds the minimum value in a column  */
  def minBy[V: Numeric](f: T => Expr[V])(implicit qr: Queryable[Expr[V], V]): Expr[V] = v.queryExpr(implicit ctx => usql"MIN(${f(v.expr)})")

  /** Finds the maximum value in a column  */
  def maxBy[V: Numeric](f: T => Expr[V])(implicit qr: Queryable[Expr[V], V]): Expr[V] = v.queryExpr(implicit ctx => usql"MAX(${f(v.expr)})")

  /** Computes the average value of a column */
  def avgBy[V: Numeric](f: T => Expr[V])(implicit qr: Queryable[Expr[V], V]): Expr[V] = v.queryExpr(implicit ctx => usql"AVG(${f(v.expr)})")

  /** TRUE if any value in a set is TRUE */
  def any(f: T => Expr[Boolean]): Expr[Boolean] = v.queryExpr(implicit ctx => usql"ANY(${f(v.expr)})")

  /** TRUE if all values in a set are TRUE */
  def all(f: T => Expr[Boolean]): Expr[Boolean] = v.queryExpr(implicit ctx => usql"ALL(${f(v.expr)})")

  /** TRUE if the operand is equal to one of a list of expressions or one or more rows returned by a subquery */
  //    def contains(e: Expr[_]): Expr[Boolean] = v.queryExpr(implicit ctx => usql"ALL($e in $v})")
}