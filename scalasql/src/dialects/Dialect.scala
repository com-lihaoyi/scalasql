package scalasql.dialects

import scalasql.operations.{CaseWhen, DbApiOps, TableOps, WindowExpr}
import scalasql.query.{Aggregatable, Expr, JoinNullable, Select, WithCte, WithCteRef}
import scalasql.renderer.SqlStr
import scalasql.utils.OptionPickler
import scalasql.{DbApi, Queryable, Table, TypeMapper, operations}

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
  implicit def DbApiOpsConv(db: => DbApi): DbApiOps = new DbApiOps()

  implicit class WindowExtensions[T](e: Expr[T]) {
    def over = new WindowExpr[T](e, None, None, Nil, None, None, None)
  }
  // This is necessary for `runSql` to work.
  implicit def ExprQueryable[T](
      implicit valueReader0: OptionPickler.Reader[T],
      mt: TypeMapper[T]
  ): Queryable.Row[_, T] = {
    new Expr.toExprable[Expr, T]()
  }
}
