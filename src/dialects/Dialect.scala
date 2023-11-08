package scalasql.dialects

import scalasql.operations.{CaseWhen, TableOps}
import scalasql.query.{Aggregatable, Expr, Select}
import scalasql.{MappedType, Queryable, Table, operations}

/**
 * Base type for all SQL dialects. A Dialect proides extension methods, extension operators,
 * and custom implementations of various query classes that may differ between databases
 */
trait Dialect extends DialectConfig {
  implicit def ExprBooleanOpsConv(v: Expr[Boolean]): operations.ExprBooleanOps =
    new operations.ExprBooleanOps(v)
  implicit def ExprNumericOpsConv[T: Numeric: MappedType](
      v: Expr[T]
  ): operations.ExprNumericOps[T] = new operations.ExprNumericOps(v)
  implicit def ExprOpsConv(v: Expr[_]): operations.ExprOps = new operations.ExprOps(v)
  implicit def ExprOptionOpsConv[T: MappedType](v: Expr[Option[T]]): operations.ExprOptionOps[T] =
    new operations.ExprOptionOps(v)
  implicit def ExprStringOpsConv(v: Expr[String]): operations.ExprStringOps
  implicit def AggNumericOpsConv[V: Numeric: MappedType](v: Aggregatable[Expr[V]])(
      implicit qr: Queryable.Row[Expr[V], V]
  ): operations.AggNumericOps[V] = new operations.AggNumericOps(v)

  implicit def AggOpsConv[T](v: Aggregatable[T])(
      implicit qr: Queryable.Row[T, _]
  ): operations.AggOps[T] = new operations.AggOps(v)

  implicit def SelectOpsConv[T](v: Select[T, _]): operations.SelectOps[T] =
    new operations.SelectOps(v)

  implicit def TableOpsConv[V[_[_]]](t: Table[V]): TableOps[V] = new TableOps(t)

  def caseWhen[T: MappedType](values: (Expr[Boolean], Expr[T])*) = new CaseWhen(values)
}
