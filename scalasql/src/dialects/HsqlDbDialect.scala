package scalasql.dialects

import scalasql.{TypeMapper, operations}
import scalasql.query.Expr
import scalasql.renderer.SqlStr.SqlStringSyntax

trait HsqlDbDialect extends Dialect {
  def defaultQueryableSuffix = " FROM (VALUES (0))"
  def castParams = true

  override implicit def ExprStringOpsConv(v: Expr[String]): HsqlDbDialect.ExprStringOps =
    new HsqlDbDialect.ExprStringOps(v)
  override implicit def ExprNumericOpsConv[T: Numeric: TypeMapper](
      v: Expr[T]
  ): HsqlDbDialect.ExprNumericOps[T] = new HsqlDbDialect.ExprNumericOps(v)

  override def values[T: TypeMapper](ts: Seq[T]) = new HsqlDbDialect.Values(ts)
}

object HsqlDbDialect extends HsqlDbDialect {
  class ExprStringOps(val v: Expr[String])
      extends operations.ExprStringOps(v)
      with TrimOps
      with PadOps {
    def indexOf(x: Expr[String]): Expr[Int] = Expr { implicit ctx => sql"INSTR($v, $x)" }
    def reverse: Expr[String] = Expr { implicit ctx => sql"REVERSE($v)" }
  }

  class ExprNumericOps[T: Numeric: TypeMapper](val v: Expr[T])
      extends operations.ExprNumericOps[T](v)
      with BitwiseFunctionOps[T]

  class Values[T: TypeMapper](ts: Seq[T]) extends scalasql.query.Values[T](ts) {

    override protected def expr: Expr[T] = Expr { implicit ctx => sql"c1" }
  }
}
