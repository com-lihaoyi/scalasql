package scalasql.dialects

import scalasql.core.{Aggregatable, Queryable, Sql, TypeMapper, WithSqlExpr}
import scalasql.operations
import scalasql.query.{JoinOps, Joinable, LateralJoinOps, Select}
import scalasql.core.SqlStr
import scalasql.core.SqlStr.SqlStringSyntax
import scalasql.operations.{PadOps, TrimOps}

trait PostgresDialect extends Dialect with ReturningDialect with OnConflictOps {

  protected def dialectCastParams = false

  override implicit def ByteType: TypeMapper[Byte] = new PostgresByteType
  class PostgresByteType extends ByteType { override def castTypeString = "INTEGER" }

  override implicit def StringType: TypeMapper[String] = new PostgresStringType
  class PostgresStringType extends StringType { override def castTypeString = "VARCHAR" }

  override implicit def SqlStringOpsConv(v: Sql[String]): PostgresDialect.SqlStringOps =
    new PostgresDialect.SqlStringOps(v)

  implicit def LateralJoinOpsConv[C[_, _], Q, R](wrapped: JoinOps[C, Q, R] with Joinable[Q, R])(
      implicit qr: Queryable.Row[Q, R]
  ) = new LateralJoinOps(wrapped)

  implicit def SqlAggOpsConv[T](v: Aggregatable[Sql[T]]): operations.SqlAggOps[T] =
    new PostgresDialect.SqlAggOps(v)

  implicit class SelectDistinctOnConv[Q, R](r: Select[Q, R]) {

    /**
     * SELECT DISTINCT ON ( expression [, ...] ) keeps only the first row of each set of rows
     * where the given expressions evaluate to equal. The DISTINCT ON expressions are
     * interpreted using the same rules as for ORDER BY (see above). Note that the “first
     * row” of each set is unpredictable unless ORDER BY is used to ensure that the desired
     * row appears first. For example:
     */
    def distinctOn(f: Q => Sql[_]): Select[Q, R] = {
      Select.withExprPrefix(r, true, implicit ctx => sql"DISTINCT ON (${f(WithSqlExpr.get(r))})")
    }
  }
}

object PostgresDialect extends PostgresDialect {
  class SqlAggOps[T](v: Aggregatable[Sql[T]]) extends scalasql.operations.SqlAggOps[T](v) {
    def mkString(sep: Sql[String] = null)(implicit tm: TypeMapper[T]): Sql[String] = {
      val sepRender = Option(sep).getOrElse(sql"''")
      v.queryExpr(expr => implicit ctx => sql"STRING_AGG($expr || '', $sepRender)")
    }
  }
  class SqlStringOps(protected val v: Sql[String])
      extends operations.SqlStringOps(v)
      with TrimOps
      with PadOps {
    def indexOf(x: Sql[String]): Sql[Int] = Sql { implicit ctx => sql"POSITION($x IN $v)" }

    def reverse: Sql[String] = Sql { implicit ctx => sql"REVERSE($v)" }
  }
}
