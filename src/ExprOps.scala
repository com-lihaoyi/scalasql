package usql

import renderer.SqlStr.SqlStringSyntax
import usql.query.{Expr, Aggregatable}

object ExprOps extends ExprOps
trait ExprOps {
  implicit def ExprBooleanOpsConv(v: Expr[Boolean]): operations.ExprBooleanOps = new operations.ExprBooleanOps(v)
  implicit def ExprIntOpsConv[T: Numeric](v: Expr[T]): operations.ExprNumericOps[T] = new operations.ExprNumericOps(v)
  implicit def ExprOpsConv(v: Expr[_]): operations.ExprOps = new operations.ExprOps(v)
  implicit def ExprStringOpsConv(v: Expr[String]): operations.ExprStringOps = new operations.ExprStringOps(v)

  implicit def ExprSeqNumericOpsConv[V: Numeric](v: Aggregatable[Expr[V]])
                                                (implicit qr: Queryable[Expr[V], V]): operations.ExprSeqNumericOps[V] =
    new operations.ExprSeqNumericOps(v)

  implicit def ExprSeqOpsConv[T](v: Aggregatable[T])
                                (implicit qr: Queryable[T, _]): operations.ExprSeqOps[T] =
    new operations.ExprSeqOps(v)
}

