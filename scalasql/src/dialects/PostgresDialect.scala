package scalasql.dialects

import scalasql.core.{
  Aggregatable,
  Expr,
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

  def castParams = false

  def escape(str: String) = s""""$str""""

  def supportSavepointRelease = true

  override implicit def ByteType: TypeMapper[Byte] = new PostgresByteType
  class PostgresByteType extends ByteType { override def castTypeString = "INTEGER" }

  override implicit def StringType: TypeMapper[String] = new PostgresStringType
  class PostgresStringType extends StringType { override def castTypeString = "VARCHAR" }

  implicit def UjsonValueType: TypeMapper[ujson.Value] = new UjsonValueType
  class UjsonValueType extends TypeMapper[ujson.Value] {
    def jdbcType = java.sql.JDBCType.OTHER
    def get(r: java.sql.ResultSet, idx: Int) = {
      val str = r.getString(idx)
      if (str == null) ujson.Null
      else ujson.read(str)
    }
    def put(r: java.sql.PreparedStatement, idx: Int, v: ujson.Value) =
      r.setObject(idx, v.toString(), java.sql.Types.OTHER)
  }

  override implicit def ExprStringOpsConv(v: Expr[String]): PostgresDialect.ExprStringOps[String] =
    new PostgresDialect.ExprStringOps(v)

  implicit def ExprJsonOpsConv(v: Expr[ujson.Value]): PostgresDialect.ExprJsonOps =
    new PostgresDialect.ExprJsonOps(v)

  implicit def UjsonQueryable: Queryable.Row[Expr[ujson.Value], ujson.Value] = ExprQueryable(UjsonValueType)

  override implicit def ExprBlobOpsConv(
      v: Expr[geny.Bytes]
  ): PostgresDialect.ExprStringLikeOps[geny.Bytes] =
    new PostgresDialect.ExprStringOps(v)

  implicit def LateralJoinOpsConv[C[_, _], Q, R](wrapped: JoinOps[C, Q, R] & Joinable[Q, R])(
      implicit qr: Queryable.Row[Q, R]
  ): LateralJoinOps[C, Q, R] = new LateralJoinOps(wrapped)

  implicit def ExprAggOpsConv[T](v: Aggregatable[Expr[T]]): operations.ExprAggOps[T] =
    new PostgresDialect.ExprAggOps(v)

  implicit class SelectDistinctOnConv[Q, R](r: Select[Q, R]) {

    /**
     * SELECT DISTINCT ON ( expression [, ...] ) keeps only the first row of each set of rows
     * where the given expressions evaluate to equal. The DISTINCT ON expressions are
     * interpreted using the same rules as for ORDER BY (see above). Note that the “first
     * row” of each set is unpredictable unless ORDER BY is used to ensure that the desired
     * row appears first. For example:
     */
    def distinctOn(one: Q => Expr[?], more: (Q => Expr[?])*): Select[Q, R] = Select.withExprPrefix(
      r,
      true,
      implicit ctx =>
        sql"DISTINCT ON (${SqlStr.join((one +: more).map(f => sql"${f(WithSqlExpr.get(r))}"), SqlStr.commaSep)})"
    )
  }

  implicit class SelectForUpdateConv[Q, R](r: Select[Q, R]) {

    /**
     * SELECT .. FOR UPDATE acquires an exclusive lock, blocking other transactions from
     * modifying or locking the selected rows, which is for managing concurrent transactions
     * and ensuring data consistency in multi-step operations.
     */
    def forUpdate: Select[Q, R] =
      Select.withExprSuffix(r, true, _ => sql" FOR UPDATE")

    /**
     * SELECT ... FOR NO KEY UPDATE: A weaker lock that doesn't block inserts into child
     * tables with foreign key references
     */
    def forNoKeyUpdate: Select[Q, R] =
      Select.withExprSuffix(r, true, _ => sql" FOR NO KEY UPDATE")

    /**
     * SELECT ... FOR SHARE: Locks the selected rows for reading, allowing other transactions
     * to read but not modify the locked rows.
     */
    def forShare: Select[Q, R] =
      Select.withExprSuffix(r, true, _ => sql" FOR SHARE")

    /** SELECT ... FOR KEY SHARE: The weakest lock, only conflicts with FOR UPDATE */
    def forKeyShare: Select[Q, R] =
      Select.withExprSuffix(r, true, _ => sql" FOR KEY SHARE")

    /**
     * SELECT ... FOR UPDATE NOWAIT: Immediately returns an error if the selected rows are
     * already locked, instead of waiting
     */
    def forUpdateNoWait: Select[Q, R] =
      Select.withExprSuffix(r, true, _ => sql" FOR UPDATE NOWAIT")

    /**
     * SELECT ... FOR UPDATE SKIP LOCKED: Skips any rows that are already locked by other
     * transactions, instead of waiting
     */
    def forUpdateSkipLocked: Select[Q, R] =
      Select.withExprSuffix(r, true, _ => sql" FOR UPDATE SKIP LOCKED")
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
    def format(template: Expr[String], values: Expr[?]*): Expr[String] = Expr { implicit ctx =>
      sql"FORMAT($template, ${SqlStr.join(values.map(v => sql"$v"), SqlStr.commaSep)})"
    }

    /**
     * Returns a random value in the range 0.0 <= x < 1.0
     */
    def random: Expr[Double] = Expr { _ => sql"RANDOM()" }
  }

  class ExprJsonOps(val v: Expr[ujson.Value]) extends scalasql.operations.ExprOps(v) {
    // -> integer
    def ->(n: Expr[Int]): Expr[ujson.Value] = Expr { implicit ctx => sql"$v -> $n" }
    // -> text
    def ->(k: Expr[String])(implicit d: DummyImplicit): Expr[ujson.Value] = Expr { implicit ctx =>
      sql"$v -> $k"
    }
    // ->> integer
    def ->>(n: Expr[Int]): Expr[String] = Expr { implicit ctx => sql"$v ->> $n" }
    // ->> text
    def ->>(k: Expr[String])(implicit d: DummyImplicit): Expr[String] = Expr { implicit ctx =>
      sql"$v ->> $k"
    }

    def #>(path: Expr[String]*): Expr[ujson.Value] = Expr { implicit ctx =>
      val array = sql"ARRAY[${SqlStr.join(path.map(p => sql"$p"), sql", ")}]"
      sql"$v #> $array"
    }

    def #>>(path: Expr[String]*): Expr[String] = Expr { implicit ctx =>
      val array = sql"ARRAY[${SqlStr.join(path.map(p => sql"$p"), sql", ")}]"
      sql"$v #>> $array"
    }

    def @>(other: Expr[ujson.Value]): Expr[Boolean] = Expr { implicit ctx => sql"$v @> $other" }
    def <@(other: Expr[ujson.Value]): Expr[Boolean] = Expr { implicit ctx => sql"$v <@ $other" }

    def ?(key: Expr[String]): Expr[Boolean] = Expr { implicit ctx => sql"jsonb_exists($v, $key)" }

    def ?|(keys: Expr[String]*): Expr[Boolean] = Expr { implicit ctx =>
      val array = sql"ARRAY[${SqlStr.join(keys.map(p => sql"$p"), sql", ")}]"
      sql"jsonb_exists_any($v, $array)"
    }

    def ?&(keys: Expr[String]*): Expr[Boolean] = Expr { implicit ctx =>
      val array = sql"ARRAY[${SqlStr.join(keys.map(p => sql"$p"), sql", ")}]"
      sql"jsonb_exists_all($v, $array)"
    }

    def ||(other: Expr[ujson.Value]): Expr[ujson.Value] = Expr { implicit ctx => sql"$v || $other" }

    def -(key: Expr[String]): Expr[ujson.Value] = Expr { implicit ctx => sql"$v - $key" }
    def -(index: Expr[Int])(implicit d: DummyImplicit): Expr[ujson.Value] = Expr { implicit ctx =>
      sql"$v - $index"
    }
  }

  class ExprAggOps[T](v: Aggregatable[Expr[T]]) extends scalasql.operations.ExprAggOps[T](v) {
    def mkString(sep: Expr[String] = null)(implicit tm: TypeMapper[T]): Expr[String] = {
      val sepRender = Option(sep).getOrElse(sql"''")
      v.aggregateExpr(expr => implicit ctx => sql"STRING_AGG($expr || '', $sepRender)")
    }
  }
  class ExprStringOps[T](v: Expr[T]) extends ExprStringLikeOps(v) with operations.ExprStringOps[T]
  class ExprStringLikeOps[T](protected val v: Expr[T])
      extends operations.ExprStringLikeOps(v)
      with TrimOps
      with PadOps {
    def indexOf(x: Expr[T]): Expr[Int] = Expr { implicit ctx => sql"POSITION($x IN $v)" }

    def reverse: Expr[T] = Expr { implicit ctx => sql"REVERSE($v)" }
  }
}
