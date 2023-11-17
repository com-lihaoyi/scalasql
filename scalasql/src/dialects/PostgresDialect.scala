package scalasql.dialects

import scalasql.{Queryable, TypeMapper, operations}
import scalasql.query.{Aggregatable, Expr, JoinOps, Joinable, LateralJoinOps}
import scalasql.renderer.SqlStr
import scalasql.renderer.SqlStr.SqlStringSyntax

trait PostgresDialect extends Dialect with ReturningDialect with OnConflictOps {

  def castParams = false

  override implicit def ExprOpsConv(v: Expr[_]): PostgresDialect.ExprOps =
    new PostgresDialect.ExprOps(v)

  override implicit def ExprStringOpsConv(v: Expr[String]): PostgresDialect.ExprStringOps =
    new PostgresDialect.ExprStringOps(v)

  implicit def LateralJoinOpsConv[C[_, _], Q, R](wrapped: JoinOps[C, Q, R] with Joinable[Q, R])(
      implicit qr: Queryable.Row[Q, R]
  ) = new LateralJoinOps(wrapped)

  implicit def AggExprOpsConv[T](v: Aggregatable[Expr[T]]): operations.AggExprOps[T] = new PostgresDialect.AggExprOps(v)

}

object PostgresDialect extends PostgresDialect {
  class AggExprOps[T](v: Aggregatable[Expr[T]]) extends scalasql.operations.AggExprOps[T](v) {
    def mkString(sep: Expr[String] = null)(implicit tm: TypeMapper[T]): Expr[String] = {
      val sepRender = Option(sep).getOrElse(sql"''")
      v.queryExpr(expr => implicit ctx => sql"STRING_AGG($expr || '', $sepRender)")
    }
  }
  class ExprOps(val v: Expr[_]) extends operations.ExprOps(v) {
    override def cast[V: TypeMapper]: Expr[V] = Expr { implicit ctx =>
      val s = implicitly[TypeMapper[V]] match {
        case TypeMapper.ByteType => "INTEGER"
        case TypeMapper.StringType => "VARCHAR"
        case s => s.typeString
      }

      sql"CAST($v AS ${SqlStr.raw(s)})"
    }
  }
  class ExprStringOps(val v: Expr[String])
      extends operations.ExprStringOps(v)
      with TrimOps
      with PadOps {
    def indexOf(x: Expr[String]): Expr[Int] = Expr { implicit ctx => sql"POSITION($x IN $v)" }

    def reverse: Expr[String] = Expr { implicit ctx => sql"REVERSE($v)" }
  }
}
