package scalasql.dialects

import scalasql.operations.{CaseWhen, TableOps, WindowExpr}
import scalasql.query.{Aggregatable, Expr, JoinNullable, Select}
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

  def caseWhen[T: TypeMapper](values: (Expr[Boolean], Expr[T])*) = new CaseWhen(values)

  def values[T: TypeMapper](ts: Seq[T]) = new scalasql.query.Values(ts)

  import scalasql.renderer.SqlStr.SqlStringSyntax

  def rank(): Expr[Int] = Expr{implicit ctx => sql"RANK()"}
  def rowNumber(): Expr[Int] = Expr{implicit ctx => sql"ROW_NUMBER()"}
  def denseRank(): Expr[Int] = Expr{implicit ctx => sql"DENSE_RANK()"}
  def percentRank(): Expr[Int] = Expr{implicit ctx => sql"PERCENT_RANK()"}
  def cumeDist(n: Int): Expr[Int] = Expr{implicit ctx => sql"CUME_DIST($n)"}
  def nTile(): Expr[Int] = Expr{implicit ctx => sql"N_TILE()"}

  private def lagLead[T](prefix: SqlStr,
                         e: Expr[T],
                         offset: Int, default: Expr[T]): Expr[T] = Expr { implicit ctx =>
    val args = SqlStr.join(
      Seq(Some(sql"$e"), Some(offset).filter(_ != 1).map(o => sql"$o"), Option(default).map(d => sql"$d")).flatten,
      sql", "
    )
    sql"$prefix($args)"
  }

  def lag[T](e: Expr[T], offset: Int = -1, default: Expr[T] = null): Expr[T] =
    lagLead(sql"LAG", e, offset, default)
  def lead[T](e: Expr[T], offset: Int = -1, default: Expr[T] = null): Expr[T] =
    lagLead(sql"LEAD", e, offset, default)

  def firstValue[T](e: Expr[T]): Expr[T] = Expr{implicit ctx => sql"FIRST_VALUE($e)"}
  def lastValue[T](e: Expr[T]): Expr[T] = Expr{implicit ctx => sql"LAST_VALUE($e)"}
  def nthValue[T](e: Expr[T], n: Int): Expr[T] = Expr{implicit ctx => sql"NTH_VALUE($e, $n)"}


  implicit class WindowExtensions[T](e: Expr[T]) {
    def over = new WindowExpr[T](e, None, Nil)
  }
}
