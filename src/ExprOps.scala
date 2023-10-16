package usql


import usql.query.{Aggregatable, Expr, Select}

object ExprOps extends ExprOps
trait ExprOps {
  implicit def ExprBooleanOpsConv(v: Expr[Boolean]): operations.ExprBooleanOps = new operations.ExprBooleanOps(v)
  implicit def ExprIntOpsConv[T: Numeric](v: Expr[T]): operations.ExprNumericOps[T] = new operations.ExprNumericOps(v)
  implicit def ExprOpsConv(v: Expr[_]): operations.ExprOps = new operations.ExprOps(v)
  implicit def ExprStringOpsConv(v: Expr[String]): operations.ExprStringOps = new operations.ExprStringOps(v)

  implicit def AggNumericOpsConv[V: Numeric](v: Aggregatable[Expr[V]])
                                                (implicit qr: Queryable[Expr[V], V]): operations.AggNumericOps[V] =
    new operations.AggNumericOps(v)

  implicit def AggOpsConv[T](v: Aggregatable[T])
                                (implicit qr: Queryable[T, _]): operations.AggOps[T] =
    new operations.AggOps(v)

  implicit def SelectOpsConv[T](v: Select[T]): operations.SelectOps[T] =
    new operations.SelectOps(v)
}

