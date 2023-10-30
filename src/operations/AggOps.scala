package scalasql.operations

import scalasql.{MappedType, Queryable}
import scalasql.query.{Aggregatable, Expr}
import scalasql.renderer.SqlStr.SqlStringSyntax

class AggOps[T](v: Aggregatable[T])(implicit qr: Queryable[T, _]) {

  /** Counts the rows */
  def size: Expr[Int] = v.queryExpr(expr => implicit ctx => sql"COUNT(1)")

  /** Computes the sum of column values */
  def sumBy[V: Numeric: MappedType](f: T => Expr[V])(implicit qr: Queryable[Expr[V], V]): Expr[V] =
    v.queryExpr(expr => implicit ctx => sql"SUM(${f(expr)})")

  /** Finds the minimum value in a column */
  def minBy[V: Numeric: MappedType](f: T => Expr[V])(implicit qr: Queryable[Expr[V], V]): Expr[V] =
    v.queryExpr(expr => implicit ctx => sql"MIN(${f(expr)})")

  /** Finds the maximum value in a column */
  def maxBy[V: Numeric: MappedType](f: T => Expr[V])(implicit qr: Queryable[Expr[V], V]): Expr[V] =
    v.queryExpr(expr => implicit ctx => sql"MAX(${f(expr)})")

  /** Computes the average value of a column */
  def avgBy[V: Numeric: MappedType](f: T => Expr[V])(implicit qr: Queryable[Expr[V], V]): Expr[V] =
    v.queryExpr(expr => implicit ctx => sql"AVG(${f(expr)})")

  /** Computes the sum of column values */
  def sumByOpt[V: Numeric: MappedType](f: T => Expr[V])(
      implicit qr: Queryable[Expr[V], V]
  ): Expr[Option[V]] = v.queryExpr(expr => implicit ctx => sql"SUM(${f(expr)})")

  /** Finds the minimum value in a column */
  def minByOpt[V: Numeric: MappedType](f: T => Expr[V])(
      implicit qr: Queryable[Expr[V], V]
  ): Expr[Option[V]] = v.queryExpr(expr => implicit ctx => sql"MIN(${f(expr)})")

  /** Finds the maximum value in a column */
  def maxByOpt[V: Numeric: MappedType](f: T => Expr[V])(
      implicit qr: Queryable[Expr[V], V]
  ): Expr[Option[V]] = v.queryExpr(expr => implicit ctx => sql"MAX(${f(expr)})")

  /** Computes the average value of a column */
  def avgByOpt[V: Numeric: MappedType](f: T => Expr[V])(
      implicit qr: Queryable[Expr[V], V]
  ): Expr[Option[V]] = v.queryExpr(expr => implicit ctx => sql"AVG(${f(expr)})")

  /** TRUE if any value in a set is TRUE */
  def any(f: T => Expr[Boolean]): Expr[Boolean] = v
    .queryExpr(expr => implicit ctx => sql"ANY(${f(expr)})")

  /** TRUE if all values in a set are TRUE */
  def all(f: T => Expr[Boolean]): Expr[Boolean] = v
    .queryExpr(expr => implicit ctx => sql"ALL(${f(expr)})")

  /** TRUE if the operand is equal to one of a list of expressions or one or more rows returned by a subquery */
  //    def contains(e: Expr[_]): Expr[Boolean] = v.queryExpr(implicit ctx => sql"ALL($e in $v})")
}
