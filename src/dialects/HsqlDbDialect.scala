package scalasql.dialects

import scalasql.{MappedType, operations}
import scalasql.query.Expr
import scalasql.renderer.SqlStr.SqlStringSyntax

trait HsqlDbDialect extends Dialect {
  def defaultQueryableSuffix = " FROM (VALUES (0))"
  def castParams = true

  override implicit def ExprStringOpsConv(v: Expr[String]): HsqlDbDialect.ExprStringOps =
    new HsqlDbDialect.ExprStringOps(v)
  override implicit def ExprNumericOpsConv[T: Numeric: MappedType](
      v: Expr[T]
  ): HsqlDbDialect.ExprNumericOps[T] = new HsqlDbDialect.ExprNumericOps(v)
}

object HsqlDbDialect extends HsqlDbDialect {
  class ExprStringOps(val v: Expr[String])
      extends operations.ExprStringOps(v) with TrimOps with PadOps {
    def indexOf(x: Expr[String]): Expr[Int] = Expr { implicit ctx => sql"INSTR($v, $x)" }
    def reverse: Expr[String] = Expr { implicit ctx => sql"REVERSE($v)" }
  }

  class ExprNumericOps[T: Numeric: MappedType](val v: Expr[T])
      extends operations.ExprNumericOps[T](v) with BitwiseFunctionOps[T]
}
