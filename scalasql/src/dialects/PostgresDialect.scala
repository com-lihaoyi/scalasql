package scalasql.dialects

import scalasql.{MappedType, operations}
import scalasql.query.Expr
import scalasql.renderer.SqlStr
import scalasql.renderer.SqlStr.SqlStringSyntax

trait PostgresDialect extends Dialect with ReturningDialect with OnConflictOps {
  def defaultQueryableSuffix = ""

  def castParams = false

  override implicit def ExprOpsConv(v: Expr[_]): PostgresDialect.ExprOps =
    new PostgresDialect.ExprOps(v)

  override implicit def ExprStringOpsConv(v: Expr[String]): PostgresDialect.ExprStringOps =
    new PostgresDialect.ExprStringOps(v)
}

object PostgresDialect extends PostgresDialect {
  class ExprOps(val v: Expr[_]) extends operations.ExprOps(v) {
    override def cast[V: MappedType]: Expr[V] = Expr { implicit ctx =>
      val s = implicitly[MappedType[V]] match {
        case MappedType.ByteType => "INTEGER"
        case MappedType.StringType => "VARCHAR"
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
