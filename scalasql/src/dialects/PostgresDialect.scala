package scalasql.dialects

import scalasql.{TypeMapper, operations}
import scalasql.query.{Expr, JoinOps, LateralJoinOps}
import scalasql.renderer.SqlStr
import scalasql.renderer.SqlStr.SqlStringSyntax

trait PostgresDialect extends Dialect with ReturningDialect with OnConflictOps {

  def castParams = false

  override implicit def ExprOpsConv(v: Expr[_]): PostgresDialect.ExprOps =
    new PostgresDialect.ExprOps(v)

  override implicit def ExprStringOpsConv(v: Expr[String]): PostgresDialect.ExprStringOps =
    new PostgresDialect.ExprStringOps(v)

  implicit def LateralJoinOpsConv[C[_, _], Q, R](wrapped: JoinOps[C, Q, R]) = new LateralJoinOps(wrapped)
}

object PostgresDialect extends PostgresDialect {
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
