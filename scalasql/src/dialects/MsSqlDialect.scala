package scalasql.dialects

import scalasql.core.{Aggregatable, DbApi, DialectTypeMappers, Expr, TypeMapper}
import scalasql.operations
import scalasql.core.SqlStr.SqlStringSyntax
import scalasql.operations.{ConcatOps, MathOps, TrimOps}

trait MsSqlDialect extends Dialect {
  override def castParams = false

  override def escape(str: String): String =
    s"[$str]"

  override implicit def IntType: TypeMapper[Int] = new MsSqlIntType
  class MsSqlIntType extends IntType { override def castTypeString = "INT" }

  override implicit def StringType: TypeMapper[String] = new MsSqlStringType
  class MsSqlStringType extends StringType { override def castTypeString = "VARCHAR" }

  override implicit def ExprStringOpsConv(v: Expr[String]): MsSqlDialect.ExprStringOps[String] =
    new MsSqlDialect.ExprStringOps(v)

  override implicit def ExprBlobOpsConv(
      v: Expr[geny.Bytes]
  ): MsSqlDialect.ExprStringLikeOps[geny.Bytes] =
    new MsSqlDialect.ExprStringLikeOps(v)

  implicit def ExprAggOpsConv[T](v: Aggregatable[Expr[T]]): operations.ExprAggOps[T] =
    new MsSqlDialect.ExprAggOps(v)

  override implicit def DbApiOpsConv(db: => DbApi): MsSqlDialect.DbApiOps =
    new MsSqlDialect.DbApiOps(this)
}

object MsSqlDialect extends MsSqlDialect {
  class DbApiOps(dialect: DialectTypeMappers)
      extends scalasql.operations.DbApiOps(dialect)
      with ConcatOps
      with MathOps

  class ExprAggOps[T](v: Aggregatable[Expr[T]]) extends scalasql.operations.ExprAggOps[T](v) {
    def mkString(sep: Expr[String] = null)(implicit tm: TypeMapper[T]): Expr[String] = {
      val sepRender = Option(sep).getOrElse(sql"''")
      v.aggregateExpr(expr => implicit ctx => sql"STRING_AGG($expr + '', $sepRender)")
    }
  }

  class ExprStringOps[T](v: Expr[T]) extends ExprStringLikeOps(v) with operations.ExprStringOps[T]
  class ExprStringLikeOps[T](protected val v: Expr[T])
      extends operations.ExprStringLikeOps(v)
      with TrimOps {

    override def +(x: Expr[T]): Expr[T] = Expr { implicit ctx => sql"($v + $x)" }

    override def startsWith(other: Expr[T]): Expr[Boolean] = Expr { implicit ctx =>
      sql"($v LIKE $other + '%')"
    }

    override def endsWith(other: Expr[T]): Expr[Boolean] = Expr { implicit ctx =>
      sql"($v LIKE '%' + $other)"
    }

    override def contains(other: Expr[T]): Expr[Boolean] = Expr { implicit ctx =>
      sql"($v LIKE '%' + $other + '%')"
    }

    override def length: Expr[Int] = Expr { implicit ctx => sql"LEN($v)" }

    override def octetLength: Expr[Int] = Expr { implicit ctx => sql"DATALENGTH($v)" }

    def indexOf(x: Expr[T]): Expr[Int] = Expr { implicit ctx => sql"CHARINDEX($x, $v)" }
    def reverse: Expr[T] = Expr { implicit ctx => sql"REVERSE($v)" }
  }
}
