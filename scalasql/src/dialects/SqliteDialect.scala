package scalasql.dialects

import scalasql.core.{
  Aggregatable,
  Context,
  Expr,
  DbApi,
  DialectTypeMappers,
  Queryable,
  SqlStr,
  TypeMapper
}
import scalasql.{Sc, operations}
import scalasql.query.{CompoundSelect, GroupBy, Join, OrderBy, SubqueryRef, Table}
import scalasql.core.SqlStr.SqlStringSyntax
import scalasql.operations.TrimOps

import java.time.{Instant, LocalDate, LocalDateTime}

trait SqliteDialect extends Dialect with ReturningDialect with OnConflictOps {
  protected def dialectCastParams = false

  override implicit def LocalDateTimeType: TypeMapper[LocalDateTime] = new SqliteLocalDateTimeType
  class SqliteLocalDateTimeType extends LocalDateTimeType {
    override def castTypeString = "VARCHAR"
  }

  override implicit def LocalDateType: TypeMapper[LocalDate] = new SqliteLocalDateType
  class SqliteLocalDateType extends LocalDateType { override def castTypeString = "VARCHAR" }

  override implicit def InstantType: TypeMapper[Instant] = new SqliteInstantType
  class SqliteInstantType extends InstantType { override def castTypeString = "VARCHAR" }

  override implicit def UtilDateType: TypeMapper[java.util.Date] = new SqliteUtilDateType
  class SqliteUtilDateType extends UtilDateType { override def castTypeString = "VARCHAR" }

  override implicit def ExprStringOpsConv(v: Expr[String]): SqliteDialect.ExprStringOps[String] =
    new SqliteDialect.ExprStringOps(v)

  override implicit def ExprBlobOpsConv(
      v: Expr[geny.Bytes]
  ): SqliteDialect.ExprStringLikeOps[geny.Bytes] =
    new SqliteDialect.ExprStringLikeOps(v)

  override implicit def TableOpsConv[V[_[_]]](t: Table[V]): scalasql.dialects.TableOps[V] =
    new SqliteDialect.TableOps(t)

  implicit def ExprAggOpsConv[T](v: Aggregatable[Expr[T]]): operations.ExprAggOps[T] =
    new SqliteDialect.AggExprOps(v)

  override implicit def DbApiOpsConv(db: => DbApi): SqliteDialect.DbApiOps =
    new SqliteDialect.DbApiOps(this)
}

object SqliteDialect extends SqliteDialect {
  class DbApiOps(dialect: DialectTypeMappers) extends scalasql.operations.DbApiOps(dialect) {

    /**
     * The changes() function returns the number of database rows that were changed
     * or inserted or deleted by the most recently completed INSERT, DELETE, or
     * UPDATE statement, exclusive of statements in lower-level triggers. The
     * changes() SQL function is a wrapper around the sqlite3_changes64() C/C++
     * function and hence follows the same rules for counting changes.
     */
    def changes: Expr[Int] = Expr { _ => sql"CHANGES()" }

    /**
     * The total_changes() function returns the number of row changes caused by
     * INSERT, UPDATE or DELETE statements since the current database connection
     * was opened. This function is a wrapper around the sqlite3_total_changes64()
     * C/C++ interface.
     */
    def totalChanges: Expr[Int] = Expr { _ => sql"TOTAL_CHANGES()" }

    /**
     * The typeof(X) function returns a string that indicates the datatype of the
     * expression X: "null", "integer", "real", "text", or "blob".
     */
    def typeOf(v: Expr[?]): Expr[String] = Expr { implicit ctx => sql"TYPEOF($v)" }

    /**
     * The last_insert_rowid() function returns the ROWID of the last row insert
     * from the database connection which invoked the function. The
     * last_insert_rowid() SQL function is a wrapper around the
     * sqlite3_last_insert_rowid() C/C++ interface function.
     */
    def lastInsertRowId: Expr[Int] = Expr { _ => sql"LAST_INSERT_ROWID()" }

    /**
     * The random() function returns a pseudo-random integer between
     * -9223372036854775808 and +9223372036854775807.
     */
    def random: Expr[Long] = Expr { _ => sql"RANDOM()" }

    /**
     * The randomblob(N) function return an N-byte blob containing pseudo-random bytes.
     * If N is less than 1 then a 1-byte random blob is returned.
     *
     * Hint: applications can generate globally unique identifiers using this function
     * together with hex() and/or lower() like this:
     *
     * hex(randomblob(16))
     * lower(hex(randomblob(16)))
     */
    def randomBlob(n: Expr[Int]): Expr[geny.Bytes] = Expr { implicit ctx => sql"RANDOMBLOB($n)" }

    /**
     * The char(X1,X2,...,XN) function returns a string composed of characters
     * having the unicode code point values of the given integers
     */
    def char(values: Expr[Int]*): Expr[String] = Expr { implicit ctx =>
      sql"CHAR(${SqlStr.join(values.map(v => sql"$v"), SqlStr.commaSep)})"
    }

    /**
     * The format(FORMAT,...) SQL function works like the sqlite3_mprintf() C-language
     * function and the printf() function from the standard C library. The first
     * argument is a format string that specifies how to construct the output string
     * using values taken from subsequent arguments. If the FORMAT argument is missing
     * or NULL then the result is NULL. The %n format is silently ignored and does not
     * consume an argument. The %p format is an alias for %X. The %z format is
     * interchangeable with %s. If there are too few arguments in the argument list,
     * missing arguments are assumed to have a NULL value, which is translated into 0 or
     * 0.0 for numeric formats or an empty string for %s. See the built-in printf()
     * documentation for additional information.
     */
    def format(template: Expr[String], values: Expr[?]*): Expr[String] = Expr { implicit ctx =>
      sql"FORMAT($template, ${SqlStr.join(values.map(v => sql"$v"), SqlStr.commaSep)})"
    }

    /**
     * The hex() function interprets its argument as a BLOB and returns a string which
     * is the upper-case hexadecimal rendering of the content of that blob.
     *
     * If the argument X in "hex(X)" is an integer or floating point number, then
     * "interprets its argument as a BLOB" means that the binary number is first converted
     * into a UTF8 text representation, then that text is interpreted as a BLOB. Hence,
     * "hex(12345678)" renders as "3132333435363738" not the binary representation of
     * the integer value "0000000000BC614E".
     */
    def hex(value: Expr[?]): Expr[String] = Expr { implicit ctx => sql"HEX($value)" }

    /**
     * The unhex(X,Y) function returns a BLOB value which is the decoding of the
     * hexadecimal string X. If X contains any characters that are not hexadecimal
     * digits and which are not in Y, then unhex(X,Y) returns NULL. If Y is omitted,
     * it is understood to be an empty string and hence X must be a pure hexadecimal
     * string. All hexadecimal digits in X must occur in pairs, with both digits of
     * each pair beginning immediately adjacent to one another, or else unhex(X,Y)
     * returns NULL. If either parameter X or Y is NULL, then unhex(X,Y) returns NULL.
     * The X input may contain an arbitrary mix of upper and lower case hexadecimal
     * digits. Hexadecimal digits in Y have no affect on the translation of X. Only
     * characters in Y that are not hexadecimal digits are ignored in X.
     */
    def unhex(value: Expr[String]): Expr[geny.Bytes] = Expr { implicit ctx => sql"UNHEX($value)" }

    /**
     * The unhex(X,Y) function returns a BLOB value which is the decoding of the
     * hexadecimal string X. If X contains any characters that are not hexadecimal
     * digits and which are not in Y, then unhex(X,Y) returns NULL. If Y is omitted,
     * it is understood to be an empty string and hence X must be a pure hexadecimal
     * string. All hexadecimal digits in X must occur in pairs, with both digits of
     * each pair beginning immediately adjacent to one another, or else unhex(X,Y)
     * returns NULL. If either parameter X or Y is NULL, then unhex(X,Y) returns NULL.
     * The X input may contain an arbitrary mix of upper and lower case hexadecimal
     * digits. Hexadecimal digits in Y have no affect on the translation of X. Only
     * characters in Y that are not hexadecimal digits are ignored in X.
     */
    def zeroBlob(n: Expr[Int]): Expr[geny.Bytes] = Expr { implicit ctx => sql"ZEROBLOB($n)" }

  }
  class AggExprOps[T](v: Aggregatable[Expr[T]]) extends scalasql.operations.ExprAggOps[T](v) {

    /** TRUE if all values in a set are TRUE */
    def mkString(sep: Expr[String] = null)(implicit tm: TypeMapper[T]): Expr[String] = {
      val sepRender = Option(sep).getOrElse(sql"''")
      v.aggregateExpr(expr => implicit ctx => sql"GROUP_CONCAT($expr || '', $sepRender)")
    }
  }

  class ExprStringOps[T](v: Expr[T]) extends ExprStringLikeOps(v) with operations.ExprStringOps[T]
  class ExprStringLikeOps[T](protected val v: Expr[T])
      extends operations.ExprStringLikeOps(v)
      with TrimOps {
    def indexOf(x: Expr[T]): Expr[Int] = Expr { implicit ctx => sql"INSTR($v, $x)" }
    def glob(x: Expr[T]): Expr[Boolean] = Expr { implicit ctx => sql"GLOB($v, $x)" }
  }

  class TableOps[V[_[_]]](t: Table[V]) extends scalasql.dialects.TableOps[V](t) {

    protected override def joinableToSelect: Select[V[Expr], V[Sc]] = {
      val ref = Table.ref(t)
      new SimpleSelect(
        Table.metadata(t).vExpr(ref, dialectSelf).asInstanceOf[V[Expr]],
        None,
        None,
        false,
        Seq(ref),
        Nil,
        Nil,
        None
      )(
        t.containerQr
      )
    }
  }

  trait Select[Q, R] extends scalasql.query.Select[Q, R] {
    override def newCompoundSelect[Q, R](
        lhs: scalasql.query.SimpleSelect[Q, R],
        compoundOps: Seq[CompoundSelect.Op[Q, R]],
        orderBy: Seq[OrderBy],
        limit: Option[Int],
        offset: Option[Int]
    )(
        implicit qr: Queryable.Row[Q, R],
        dialect: scalasql.core.DialectTypeMappers
    ): scalasql.query.CompoundSelect[Q, R] = {
      new CompoundSelect(lhs, compoundOps, orderBy, limit, offset)
    }

    override def newSimpleSelect[Q, R](
        expr: Q,
        exprPrefix: Option[Context => SqlStr],
        exprSuffix: Option[Context => SqlStr],
        preserveAll: Boolean,
        from: Seq[Context.From],
        joins: Seq[Join],
        where: Seq[Expr[?]],
        groupBy0: Option[GroupBy]
    )(
        implicit qr: Queryable.Row[Q, R],
        dialect: scalasql.core.DialectTypeMappers
    ): scalasql.query.SimpleSelect[Q, R] = {
      new SimpleSelect(expr, exprPrefix, exprSuffix, preserveAll, from, joins, where, groupBy0)
    }
  }

  class SimpleSelect[Q, R](
      expr: Q,
      exprPrefix: Option[Context => SqlStr],
      exprSuffix: Option[Context => SqlStr],
      preserveAll: Boolean,
      from: Seq[Context.From],
      joins: Seq[Join],
      where: Seq[Expr[?]],
      groupBy0: Option[GroupBy]
  )(implicit qr: Queryable.Row[Q, R])
      extends scalasql.query.SimpleSelect(
        expr,
        exprPrefix,
        exprSuffix,
        preserveAll,
        from,
        joins,
        where,
        groupBy0
      )
      with Select[Q, R]

  class CompoundSelect[Q, R](
      lhs: scalasql.query.SimpleSelect[Q, R],
      compoundOps: Seq[scalasql.query.CompoundSelect.Op[Q, R]],
      orderBy: Seq[OrderBy],
      limit: Option[Int],
      offset: Option[Int]
  )(implicit qr: Queryable.Row[Q, R])
      extends scalasql.query.CompoundSelect(lhs, compoundOps, orderBy, limit, offset)
      with Select[Q, R] {
    protected override def selectRenderer(prevContext: Context): SubqueryRef.Wrapped.Renderer = {
      new CompoundSelectRenderer(this, prevContext)
    }
  }

  class CompoundSelectRenderer[Q, R](
      query: scalasql.query.CompoundSelect[Q, R],
      prevContext: Context
  ) extends scalasql.query.CompoundSelect.Renderer(query, prevContext) {
    override lazy val limitOpt = SqlStr
      .flatten(CompoundSelectRendererForceLimit.limitToSqlStr(query.limit, query.offset))
  }

}
