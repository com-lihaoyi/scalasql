package scalasql.dialects

import scalasql.core.{
  Aggregatable,
  Db,
  DbApi,
  DialectTypeMappers,
  Queryable,
  SqlStr,
  TypeMapper,
  WithSqlExpr
}
import scalasql.operations
import scalasql.query.{JoinOps, Joinable, LateralJoinOps, Select}
import scalasql.core.SqlStr.SqlStringSyntax
import scalasql.operations.{ConcatOps, HyperbolicMathOps, MathOps, PadOps, TrimOps}

trait PostgresDialect extends Dialect with ReturningDialect with OnConflictOps {

  protected def dialectCastParams = false

  override implicit def ByteType: TypeMapper[Byte] = new PostgresByteType
  class PostgresByteType extends ByteType { override def castTypeString = "INTEGER" }

  override implicit def StringType: TypeMapper[String] = new PostgresStringType
  class PostgresStringType extends StringType { override def castTypeString = "VARCHAR" }

  override implicit def DbStringOpsConv(v: Db[String]): PostgresDialect.DbStringOps[String] =
    new PostgresDialect.DbStringOps(v)

  override implicit def DbBlobOpsConv(
      v: Db[geny.Bytes]
  ): PostgresDialect.DbStringLikeOps[geny.Bytes] =
    new PostgresDialect.DbStringOps(v)

  implicit def LateralJoinOpsConv[C[_, _], Q, R](wrapped: JoinOps[C, Q, R] with Joinable[Q, R])(
      implicit qr: Queryable.Row[Q, R]
  ) = new LateralJoinOps(wrapped)

  implicit def DbAggOpsConv[T](v: Aggregatable[Db[T]]): operations.DbAggOps[T] =
    new PostgresDialect.DbAggOps(v)

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

  override implicit def DbApiOpsConv(db: => DbApi): PostgresDialect.DbApiOps =
    new PostgresDialect.DbApiOps(this)
}

object PostgresDialect extends PostgresDialect {

  class DbApiOps(dialect: DialectTypeMappers)
      extends scalasql.operations.DbApiOps(dialect)
      with ConcatOps
      with MathOps
      with HyperbolicMathOps {

    /**
     * Formats arguments according to a format string. This function is similar to the C function sprintf.
     */
    def format(template: Db[String], values: Db[_]*): Db[String] = Db { implicit ctx =>
      sql"FORMAT($template, ${SqlStr.join(values.map(v => sql"$v"), SqlStr.commaSep)})"
    }

    /**
     * Returns a random value in the range 0.0 <= x < 1.0
     */
    def random: Db[Double] = Db { implicit ctx => sql"RANDOM()" }
  }

  class DbAggOps[T](v: Aggregatable[Db[T]]) extends scalasql.operations.DbAggOps[T](v) {
    def mkString(sep: Db[String] = null)(implicit tm: TypeMapper[T]): Db[String] = {
      val sepRender = Option(sep).getOrElse(sql"''")
      v.aggregateExpr(expr => implicit ctx => sql"STRING_AGG($expr || '', $sepRender)")
    }
  }
  class DbStringOps[T](v: Db[T]) extends DbStringLikeOps(v) with operations.DbStringOps[T]
  class DbStringLikeOps[T](protected val v: Db[T])
      extends operations.DbStringLikeOps(v)
      with TrimOps
      with PadOps {
    def indexOf(x: Db[T]): Db[Int] = Db { implicit ctx => sql"POSITION($x IN $v)" }

    def reverse: Db[T] = Db { implicit ctx => sql"REVERSE($v)" }
  }
}
