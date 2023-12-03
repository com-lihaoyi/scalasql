package scalasql.dialects

import scalasql.{Queryable, TypeMapper, operations}
import scalasql.query.{Aggregatable, Sql, JoinOps, Joinable, LateralJoinOps, Select, WithExpr}
import scalasql.renderer.SqlStr
import scalasql.renderer.SqlStr.SqlStringSyntax

trait PostgresDialect extends Dialect with ReturningDialect with OnConflictOps {

  protected def dialectCastParams = false

  override implicit def ByteType: TypeMapper[Byte] = new PostgresByteType
  class PostgresByteType extends ByteType { override def castTypeString = "INTEGER" }

  override implicit def StringType: TypeMapper[String] = new PostgresStringType
  class PostgresStringType extends StringType { override def castTypeString = "VARCHAR" }

  override implicit def ExprStringOpsConv(v: Sql[String]): PostgresDialect.ExprStringOps =
    new PostgresDialect.ExprStringOps(v)

  implicit def LateralJoinOpsConv[C[_, _], Q, R](wrapped: JoinOps[C, Q, R] with Joinable[Q, R])(
      implicit qr: Queryable.Row[Q, R]
  ) = new LateralJoinOps(wrapped)

  implicit def AggExprOpsConv[T](v: Aggregatable[Sql[T]]): operations.AggExprOps[T] =
    new PostgresDialect.AggExprOps(v)

  implicit class SelectDistinctOnConv[Q, R](r: Select[Q, R]) {

    /**
     * SELECT DISTINCT ON ( expression [, ...] ) keeps only the first row of each set of rows
     * where the given expressions evaluate to equal. The DISTINCT ON expressions are
     * interpreted using the same rules as for ORDER BY (see above). Note that the “first
     * row” of each set is unpredictable unless ORDER BY is used to ensure that the desired
     * row appears first. For example:
     */
    def distinctOn(f: Q => Sql[_]): Select[Q, R] = {
      Select.selectWithExprPrefix(r, implicit ctx => sql"DISTINCT ON (${f(WithExpr.get(r))})")
    }
  }
}

object PostgresDialect extends PostgresDialect {
  class AggExprOps[T](v: Aggregatable[Sql[T]]) extends scalasql.operations.AggExprOps[T](v) {
    def mkString(sep: Sql[String] = null)(implicit tm: TypeMapper[T]): Sql[String] = {
      val sepRender = Option(sep).getOrElse(sql"''")
      v.queryExpr(expr => implicit ctx => sql"STRING_AGG($expr || '', $sepRender)")
    }
  }
  class ExprStringOps(protected val v: Sql[String])
      extends operations.ExprStringOps(v)
      with TrimOps
      with PadOps {
    def indexOf(x: Sql[String]): Sql[Int] = Sql { implicit ctx => sql"POSITION($x IN $v)" }

    def reverse: Sql[String] = Sql { implicit ctx => sql"REVERSE($v)" }
  }
}
