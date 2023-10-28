package scalasql.dialects

import scalasql.{Column, MappedType, dialects, operations}
import scalasql.query.{Expr, InsertSelect, InsertValues, Query}
import scalasql.renderer.{Context, SqlStr}
import scalasql.renderer.SqlStr.SqlStringSyntax

object H2Dialect extends H2Dialect {
  class ExprStringOps(val v: Expr[String]) extends operations.ExprStringOps(v) with TrimOps
      with PadOps {
    def indexOf(x: Expr[String]): Expr[Int] = Expr { implicit ctx => sql"INSTR($v, $x)" }
  }

  class ExprNumericOps[T: Numeric: MappedType](val v: Expr[T])
      extends operations.ExprNumericOps[T](v) with BitwiseFunctionOps[T]
}
trait H2Dialect extends Dialect {
  override implicit def ExprStringOpsConv(v: Expr[String]): H2Dialect.ExprStringOps =
    new H2Dialect.ExprStringOps(v)
  override implicit def ExprNumericOpsConv[T: Numeric: MappedType](v: Expr[T])
      : H2Dialect.ExprNumericOps[T] =
    new H2Dialect.ExprNumericOps(v)
}
