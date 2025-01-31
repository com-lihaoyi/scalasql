package scalasql.operations

import scalasql.core.DialectTypeMappers
import scalasql.core.{Queryable, TypeMapper, Expr}
import scalasql.core.Aggregatable
import scalasql.core.SqlStr.SqlStringSyntax

class AggOps[T](v: Aggregatable[T])(implicit qr: Queryable.Row[T, ?], dialect: DialectTypeMappers) {
  import dialect._

  /** Counts the rows */
  def size: Expr[Int] = v.aggregateExpr(_ => _ => sql"COUNT(1)")

  /** Computes the sum of column values */
  def sumBy[V: TypeMapper](f: T => Expr[V])(
      implicit qr: Queryable.Row[Expr[V], V]
  ): Expr[V] = v.aggregateExpr(expr => implicit ctx => sql"SUM(${f(expr)})")

  /** Finds the minimum value in a column */
  def minBy[V: TypeMapper](f: T => Expr[V])(
      implicit qr: Queryable.Row[Expr[V], V]
  ): Expr[V] = v.aggregateExpr(expr => implicit ctx => sql"MIN(${f(expr)})")

  /** Finds the maximum value in a column */
  def maxBy[V: TypeMapper](f: T => Expr[V])(
      implicit qr: Queryable.Row[Expr[V], V]
  ): Expr[V] = v.aggregateExpr(expr => implicit ctx => sql"MAX(${f(expr)})")

  /** Computes the average value of a column */
  def avgBy[V: TypeMapper](f: T => Expr[V])(
      implicit qr: Queryable.Row[Expr[V], V]
  ): Expr[V] = v.aggregateExpr(expr => implicit ctx => sql"AVG(${f(expr)})")

  /** Computes the sum of column values */
  def sumByOpt[V: TypeMapper](f: T => Expr[V])(
      implicit qr: Queryable.Row[Expr[V], V]
  ): Expr[Option[V]] = v.aggregateExpr(expr => implicit ctx => sql"SUM(${f(expr)})")

  /** Finds the minimum value in a column */
  def minByOpt[V: TypeMapper](f: T => Expr[V])(
      implicit qr: Queryable.Row[Expr[V], V]
  ): Expr[Option[V]] = v.aggregateExpr(expr => implicit ctx => sql"MIN(${f(expr)})")

  /** Finds the maximum value in a column */
  def maxByOpt[V: TypeMapper](f: T => Expr[V])(
      implicit qr: Queryable.Row[Expr[V], V]
  ): Expr[Option[V]] = v.aggregateExpr(expr => implicit ctx => sql"MAX(${f(expr)})")

  /** Computes the average value of a column */
  def avgByOpt[V: TypeMapper](f: T => Expr[V])(
      implicit qr: Queryable.Row[Expr[V], V]
  ): Expr[Option[V]] = v.aggregateExpr(expr => implicit ctx => sql"AVG(${f(expr)})")

  /** TRUE if any value in a set is TRUE */
  def any(f: T => Expr[Boolean]): Expr[Boolean] = v
    .aggregateExpr(expr => implicit ctx => sql"ANY(${f(expr)})")

  /** TRUE if all values in a set are TRUE */
  def all(f: T => Expr[Boolean]): Expr[Boolean] = v
    .aggregateExpr(expr => implicit ctx => sql"ALL(${f(expr)})")

  /** TRUE if the operand is equal to one of a list of expressions or one or more rows returned by a subquery */
  //    def contains(e: Expr[_]): Expr[Boolean] = v.queryExpr(implicit ctx => sql"ALL($e in $v})")
}
