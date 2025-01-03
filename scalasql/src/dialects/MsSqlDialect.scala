package scalasql.dialects

import scalasql.query.{AscDesc, GroupBy, Join, Nulls, OrderBy, SubqueryRef, Table}
import scalasql.core.{
  Aggregatable,
  Context,
  DbApi,
  DialectTypeMappers,
  Expr,
  ExprsToSql,
  LiveExprs,
  Queryable,
  SqlStr,
  TypeMapper
}
import scalasql.{Sc, operations}
import scalasql.core.SqlStr.{Renderable, SqlStringSyntax}
import scalasql.operations.{ConcatOps, MathOps, TrimOps}

import java.time.{Instant, LocalDateTime, OffsetDateTime}
import java.sql.JDBCType

trait MsSqlDialect extends Dialect {
  protected def dialectCastParams = false

  override implicit def IntType: TypeMapper[Int] = new MsSqlIntType
  class MsSqlIntType extends IntType { override def castTypeString = "INT" }

  override implicit def StringType: TypeMapper[String] = new MsSqlStringType
  class MsSqlStringType extends StringType { override def castTypeString = "VARCHAR" }

  override implicit def BooleanType: TypeMapper[Boolean] = new BooleanType
  class MsSqlBooleanType extends BooleanType { override def castTypeString = "BIT" }

  override implicit def UtilDateType: TypeMapper[java.util.Date] = new MsSqlUtilDateType
  class MsSqlUtilDateType extends UtilDateType { override def castTypeString = "DATETIME2" }

  override implicit def LocalDateTimeType: TypeMapper[LocalDateTime] = new MsSqlLocalDateTimeType
  class MsSqlLocalDateTimeType extends LocalDateTimeType {
    override def castTypeString = "DATETIME2"
  }

  override implicit def InstantType: TypeMapper[Instant] = new MsSqlInstantType
  class MsSqlInstantType extends InstantType { override def castTypeString = "DATETIME2" }

  override implicit def OffsetDateTimeType: TypeMapper[OffsetDateTime] = new MsSqlOffsetDateTimeType
  class MsSqlOffsetDateTimeType extends OffsetDateTimeType {
    override def castTypeString = "DATETIMEOFFSET"
  }

  override implicit def ExprStringOpsConv(v: Expr[String]): MsSqlDialect.ExprStringOps[String] =
    new MsSqlDialect.ExprStringOps(v)

  override implicit def ExprBlobOpsConv(
      v: Expr[geny.Bytes]
  ): MsSqlDialect.ExprStringLikeOps[geny.Bytes] =
    new MsSqlDialect.ExprStringLikeOps(v)

  override implicit def ExprNumericOpsConv[T: Numeric: TypeMapper](
      v: Expr[T]
  ): MsSqlDialect.ExprNumericOps[T] = new MsSqlDialect.ExprNumericOps(v)

  override implicit def TableOpsConv[V[_[_]]](t: Table[V]): scalasql.dialects.TableOps[V] =
    new MsSqlDialect.TableOps(t)

  implicit def ExprAggOpsConv[T](v: Aggregatable[Expr[T]]): operations.ExprAggOps[T] =
    new MsSqlDialect.ExprAggOps(v)

  override implicit def DbApiOpsConv(db: => DbApi): MsSqlDialect.DbApiOps =
    new MsSqlDialect.DbApiOps(this)

  override implicit def ExprQueryable[T](implicit mt: TypeMapper[T]): Queryable.Row[Expr[T], T] = {
    new MsSqlDialect.ExprQueryable[Expr, T]()
  }
}

object MsSqlDialect extends MsSqlDialect {
  class DbApiOps(dialect: DialectTypeMappers)
      extends scalasql.operations.DbApiOps(dialect)
      with ConcatOps
      with MathOps {
    override def ln[T: Numeric](v: Expr[T]): Expr[Double] = Expr { implicit ctx => sql"LOG($v)" }

    override def atan2[T: Numeric](v: Expr[T], y: Expr[T]): Expr[Double] = Expr { implicit ctx =>
      sql"ATN2($v, $y)"
    }
  }

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
      sql"($v LIKE CAST($other AS VARCHAR(MAX)) + '%')"
    }

    override def endsWith(other: Expr[T]): Expr[Boolean] = Expr { implicit ctx =>
      sql"($v LIKE '%' + CAST($other AS VARCHAR(MAX)))"
    }

    override def contains(other: Expr[T]): Expr[Boolean] = Expr { implicit ctx =>
      sql"($v LIKE '%' + CAST($other AS VARCHAR(MAX)) + '%')"
    }

    override def length: Expr[Int] = Expr { implicit ctx => sql"LEN($v)" }

    override def octetLength: Expr[Int] = Expr { implicit ctx => sql"DATALENGTH($v)" }

    def indexOf(x: Expr[T]): Expr[Int] = Expr { implicit ctx => sql"CHARINDEX($x, $v)" }
    def reverse: Expr[T] = Expr { implicit ctx => sql"REVERSE($v)" }
  }

  class ExprNumericOps[T: Numeric: TypeMapper](protected val v: Expr[T])
      extends operations.ExprNumericOps[T](v) {
    override def %[V: Numeric](x: Expr[V]): Expr[T] = Expr { implicit ctx => sql"$v % $x" }

    override def mod[V: Numeric](x: Expr[V]): Expr[T] = Expr { implicit ctx => sql"$v % $x" }

    override def ceil: Expr[T] = Expr { implicit ctx => sql"CEILING($v)" }
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
        compoundOps: Seq[scalasql.query.CompoundSelect.Op[Q, R]],
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
      with Select[Q, R] {
    override def take(n: Int): scalasql.query.Select[Q, R] = {
      selectWithExprPrefix(true, _ => sql"TOP($n)")
    }

    override def drop(n: Int): scalasql.query.Select[Q, R] = throw new Exception(
      ".drop must follow .sortBy"
    )

  }

  class CompoundSelect[Q, R](
      lhs: scalasql.query.SimpleSelect[Q, R],
      compoundOps: Seq[scalasql.query.CompoundSelect.Op[Q, R]],
      orderBy: Seq[OrderBy],
      limit: Option[Int],
      offset: Option[Int]
  )(implicit qr: Queryable.Row[Q, R])
      extends scalasql.query.CompoundSelect(lhs, compoundOps, orderBy, limit, offset)
      with Select[Q, R] {
    override def take(n: Int): scalasql.query.Select[Q, R] = copy(
      limit = Some(limit.fold(n)(math.min(_, n))),
      offset = offset.orElse(Some(0))
    )

    protected override def selectRenderer(prevContext: Context): SubqueryRef.Wrapped.Renderer =
      new CompoundSelectRenderer(this, prevContext)
  }

  class CompoundSelectRenderer[Q, R](
      query: scalasql.query.CompoundSelect[Q, R],
      prevContext: Context
  ) extends scalasql.query.CompoundSelect.Renderer(query, prevContext) {
    override lazy val limitOpt = SqlStr.flatten(SqlStr.opt(query.limit) { limit =>
      sql" FETCH FIRST $limit ROWS ONLY"
    })

    override lazy val offsetOpt = SqlStr.flatten(
      SqlStr.opt(query.offset.orElse(Option.when(query.limit.nonEmpty)(0))) { offset =>
        sql" OFFSET $offset ROWS"
      }
    )

    override def render(liveExprs: LiveExprs): SqlStr = {
      prerender(liveExprs) match {
        case (lhsStr, compound, sortOpt, limitOpt, offsetOpt) =>
          lhsStr + compound + sortOpt + offsetOpt + limitOpt
      }
    }

    override def orderToSqlStr(newCtx: Context) = {
      SqlStr.optSeq(query.orderBy) { orderBys =>
        val orderStr = SqlStr.join(
          orderBys.map { orderBy =>
            val exprStr = Renderable.renderSql(orderBy.expr)(newCtx)

            (orderBy.ascDesc, orderBy.nulls) match {
              case (Some(AscDesc.Asc), None | Some(Nulls.First)) => sql"$exprStr ASC"
              case (Some(AscDesc.Desc), Some(Nulls.First)) =>
                sql"IIF($exprStr IS NULL, 0, 1), $exprStr DESC"
              case (Some(AscDesc.Asc), Some(Nulls.Last)) =>
                sql"IIF($exprStr IS NULL, 1, 0), $exprStr ASC"
              case (Some(AscDesc.Desc), None | Some(Nulls.Last)) => sql"$exprStr DESC"
              case (None, None) => exprStr
              case (None, Some(Nulls.First)) => sql"IIF($exprStr IS NULL, 0, 1), $exprStr"
              case (None, Some(Nulls.Last)) => sql"IIF($exprStr IS NULL, 1, 0), $exprStr"
            }
          },
          SqlStr.commaSep
        )

        sql" ORDER BY $orderStr"
      }
    }
  }

  class ExprQueryable[E[_] <: Expr[?], T](
      implicit tm: TypeMapper[T]
  ) extends Expr.ExprQueryable[E, T] {
    override def walkExprs(q: E[T]): Seq[Expr[_]] =
      if (tm.jdbcType == JDBCType.BOOLEAN) {
        Seq(Expr[Boolean] { implicit ctx: Context => sql"CASE WHEN $q THEN 1 ELSE 0 END" })
      } else super.walkExprs(q)
  }
}
