package scalasql.dialects

import scalasql.core.{Aggregatable, Db, DbApi, DialectTypeMappers, Queryable, SqlStr, TypeMapper, WithSqlExpr}
import scalasql.operations
import scalasql.query.{JoinOps, Joinable, LateralJoinOps, Select}
import scalasql.core.SqlStr.SqlStringSyntax
import scalasql.operations.{ConcatOps, PadOps, TrimOps}

trait PostgresDialect extends Dialect with ReturningDialect with OnConflictOps {

  protected def dialectCastParams = false

  override implicit def ByteType: TypeMapper[Byte] = new PostgresByteType
  class PostgresByteType extends ByteType { override def castTypeString = "INTEGER" }

  override implicit def StringType: TypeMapper[String] = new PostgresStringType
  class PostgresStringType extends StringType { override def castTypeString = "VARCHAR" }

  override implicit def DbStringOpsConv(v: Db[String]): PostgresDialect.SqlStringOps =
    new PostgresDialect.SqlStringOps(v)

  implicit def LateralJoinOpsConv[C[_, _], Q, R](wrapped: JoinOps[C, Q, R] with Joinable[Q, R])(
      implicit qr: Queryable.Row[Q, R]
  ) = new LateralJoinOps(wrapped)

  implicit def DbAggOpsConv[T](v: Aggregatable[Db[T]]): operations.DbAggOps[T] =
    new PostgresDialect.SqlAggOps(v)

  implicit class SelectDistinctOnConv[Q, R](r: Select[Q, R]) {

    /**
     * SELECT DISTINCT ON ( expression [, ...] ) keeps only the first row of each set of rows
     * where the given expressions evaluate to equal. The DISTINCT ON expressions are
     * interpreted using the same rules as for ORDER BY (see above). Note that the “first
     * row” of each set is unpredictable unless ORDER BY is used to ensure that the desired
     * row appears first. For example:
     */
    def distinctOn(f: Q => Db[_]): Select[Q, R] = {
      Select.withExprPrefix(r, true, implicit ctx => sql"DISTINCT ON (${f(WithSqlExpr.get(r))})")
    }
  }

  override implicit def DbApiOpsConv(db: => DbApi): PostgresDialect.DbApiOps = new PostgresDialect.DbApiOps(this)
}

object PostgresDialect extends PostgresDialect {
  class DbApiOps(dialect: DialectTypeMappers) extends scalasql.operations.DbApiOps(dialect) with ConcatOps

  class SqlAggOps[T](v: Aggregatable[Db[T]]) extends scalasql.operations.DbAggOps[T](v) {
    def mkString(sep: Db[String] = null)(implicit tm: TypeMapper[T]): Db[String] = {
      val sepRender = Option(sep).getOrElse(sql"''")
      v.queryExpr(expr => implicit ctx => sql"STRING_AGG($expr || '', $sepRender)")
    }
  }
  class SqlStringOps(protected val v: Db[String])
      extends operations.DbStringOps(v)
      with TrimOps
      with PadOps {
    def indexOf(x: Db[String]): Db[Int] = Db { implicit ctx => sql"POSITION($x IN $v)" }

    def reverse: Db[String] = Db { implicit ctx => sql"REVERSE($v)" }
  }
}
