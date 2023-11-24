package scalasql.dialects

import scalasql.operations.{CaseWhen, TableOps, WindowExpr}
import scalasql.query.{
  Aggregatable,
  Expr,
  JoinNullable,
  Select,
  SimpleSelect,
  SubqueryRef,
  WithCte,
  WithCteRef,
  WithExpr
}
import scalasql.renderer.SqlStr
import scalasql.{Queryable, Table, TypeMapper, operations}

import scala.reflect.ClassTag

/**
 * Base type for all SQL dialects. A Dialect proides extension methods, extension operators,
 * and custom implementations of various query classes that may differ between databases
 */
trait Dialect extends DialectConfig {
  implicit def ExprBooleanOpsConv(v: Expr[Boolean]): operations.ExprBooleanOps =
    new operations.ExprBooleanOps(v)
  implicit def ExprNumericOpsConv[T: Numeric: TypeMapper](
      v: Expr[T]
  ): operations.ExprNumericOps[T] = new operations.ExprNumericOps(v)

  implicit def ExprOpsConv(v: Expr[_]): operations.ExprOps = new operations.ExprOps(v)

  implicit def ExprTypedOpsConv[T: ClassTag](v: Expr[T]): operations.ExprTypedOps[T] =
    new operations.ExprTypedOps(v)

  implicit def ExprOptionOpsConv[T: TypeMapper](v: Expr[Option[T]]): operations.ExprOptionOps[T] =
    new operations.ExprOptionOps(v)

  implicit def NullableExprOpsConv[T: TypeMapper](v: JoinNullable[Expr[T]]): operations.ExprOps =
    new operations.ExprOps(JoinNullable.toExpr(v))

  implicit def NullableExprOptionOpsConv[T: TypeMapper](
      v: JoinNullable[Expr[T]]
  ): operations.ExprOptionOps[T] =
    new operations.ExprOptionOps(JoinNullable.toExpr(v))

  implicit def ExprStringOpsConv(v: Expr[String]): operations.ExprStringOps

  implicit def AggNumericOpsConv[V: Numeric: TypeMapper](v: Aggregatable[Expr[V]])(
      implicit qr: Queryable.Row[Expr[V], V]
  ): operations.AggNumericOps[V] = new operations.AggNumericOps(v)

  implicit def AggOpsConv[T](v: Aggregatable[T])(
      implicit qr: Queryable.Row[T, _]
  ): operations.AggOps[T] = new operations.AggOps(v)

  implicit def AggExprOpsConv[T](v: Aggregatable[Expr[T]]): operations.AggExprOps[T]

  implicit def SelectOpsConv[T](v: Select[T, _]): operations.SelectOps[T] =
    new operations.SelectOps(v)

  implicit def TableOpsConv[V[_[_]]](t: Table[V]): TableOps[V] = new TableOps(t)

  /**
   * Creates a SQL `CASE`/`WHEN`/`ELSE` clause
   */
  def caseWhen[T: TypeMapper](values: (Expr[Boolean], Expr[T])*) = new CaseWhen(values)

  /**
   * Creates a SQL `VALUES` clause
   */
  def values[T: TypeMapper](ts: Seq[T]) = new scalasql.query.Values(ts)

  import scalasql.renderer.SqlStr.SqlStringSyntax

  /** SQL `RANK()` */
  def rank(): Expr[Int] = Expr { implicit ctx => sql"RANK()" }

  /** SQL `ROW_NUMBER()` */
  def rowNumber(): Expr[Int] = Expr { implicit ctx => sql"ROW_NUMBER()" }

  /** SQL `DENSERANK()` */
  def denseRank(): Expr[Int] = Expr { implicit ctx => sql"DENSE_RANK()" }

  /** SQL `PERCENTRANK()` */
  def percentRank(): Expr[Double] = Expr { implicit ctx => sql"PERCENT_RANK()" }

  /** SQL `CUMEDIST()` */
  def cumeDist(): Expr[Double] = Expr { implicit ctx => sql"CUME_DIST()" }

  /** SQL `NTILE()` */
  def ntile(n: Int): Expr[Int] = Expr { implicit ctx => sql"NTILE($n)" }

  private def lagLead[T](prefix: SqlStr, e: Expr[T], offset: Int, default: Expr[T]): Expr[T] =
    Expr { implicit ctx =>
      val args = SqlStr.join(
        Seq(
          Some(sql"$e"),
          Some(offset).filter(_ != -1).map(o => sql"$o"),
          Option(default).map(d => sql"$d")
        ).flatten,
        sql", "
      )

      sql"$prefix($args)"
    }

  /** SQL `LAG()` function */
  def lag[T](e: Expr[T], offset: Int = -1, default: Expr[T] = null): Expr[T] =
    lagLead(sql"LAG", e, offset, default)

  /** SQL `LEAD()` function */
  def lead[T](e: Expr[T], offset: Int = -1, default: Expr[T] = null): Expr[T] =
    lagLead(sql"LEAD", e, offset, default)

  /** SQL `FIRST_VALUE()` function */
  def firstValue[T](e: Expr[T]): Expr[T] = Expr { implicit ctx => sql"FIRST_VALUE($e)" }

  /** SQL `LAST_VALUE()` function */
  def lastValue[T](e: Expr[T]): Expr[T] = Expr { implicit ctx => sql"LAST_VALUE($e)" }

  /** SQL `NTH_VALUE()` function */
  def nthValue[T](e: Expr[T], n: Int): Expr[T] = Expr { implicit ctx => sql"NTH_VALUE($e, $n)" }

  implicit class WindowExtensions[T](e: Expr[T]) {
    def over = new WindowExpr[T](e, None, None, Nil, None, None, None)
  }

  /** Generates a SQL `WITH` common table expression clause */
  def withCte[Q, Q2, R, R2](
      lhs: Select[Q, R]
  )(block: Select[Q, R] => Select[Q2, R2])(implicit qr: Queryable.Row[Q2, R2]): Select[Q2, R2] = {
    val lhsSubQueryRef = new WithCteRef[Q, R]()
    val rhsSelect = new WithCte.Proxy[Q, R](lhs, lhsSubQueryRef, lhs.qr)

    new WithCte(lhs, lhsSubQueryRef, block(rhsSelect))
  }
}
