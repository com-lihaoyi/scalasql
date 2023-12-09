package scalasql.dialects

import scalasql.core.{Aggregatable, Queryable, Db, SqlStr, TypeMapper}
import scalasql.{Sc, dialects, operations}
import scalasql.query.{AscDesc, CompoundSelect, GroupBy, Join, Nulls, OrderBy, Select, Table}
import scalasql.core.Context
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

  override implicit def DbStringOpsConv(v: Db[String]): SqliteDialect.ExprStringOps =
    new SqliteDialect.ExprStringOps(v)

  override implicit def TableOpsConv[V[_[_]]](t: Table[V]): scalasql.dialects.TableOps[V] =
    new SqliteDialect.TableOps(t)

  implicit def DbAggOpsConv[T](v: Aggregatable[Db[T]]): operations.DbAggOps[T] =
    new SqliteDialect.AggExprOps(v)
}

object SqliteDialect extends SqliteDialect {
  class AggExprOps[T](v: Aggregatable[Db[T]]) extends scalasql.operations.DbAggOps[T](v) {

    /** TRUE if all values in a set are TRUE */
    def mkString(sep: Db[String] = null)(implicit tm: TypeMapper[T]): Db[String] = {
      val sepRender = Option(sep).getOrElse(sql"''")
      v.queryExpr(expr => implicit ctx => sql"GROUP_CONCAT($expr || '', $sepRender)")
    }
  }

  class ExprStringOps(protected val v: Db[String]) extends operations.DbStringOps(v) with TrimOps {
    def indexOf(x: Db[String]): Db[Int] = Db { implicit ctx => sql"INSTR($v, $x)" }
    def glob(x: Db[String]): Db[Int] = Db { implicit ctx => sql"GLOB($v, $x)" }
  }

  class TableOps[V[_[_]]](t: Table[V]) extends scalasql.dialects.TableOps[V](t) {

    protected override def joinableToSelect: Select[V[Db], V[Sc]] = {
      val ref = Table.ref(t)
      new SimpleSelect(
        Table.metadata(t).vExpr(ref, dialectSelf).asInstanceOf[V[Db]],
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
        preserveAll: Boolean,
        from: Seq[Context.From],
        joins: Seq[Join],
        where: Seq[Db[_]],
        groupBy0: Option[GroupBy]
    )(
        implicit qr: Queryable.Row[Q, R],
        dialect: scalasql.core.DialectTypeMappers
    ): scalasql.query.SimpleSelect[Q, R] = {
      new SimpleSelect(expr, exprPrefix, preserveAll, from, joins, where, groupBy0)
    }
  }

  class SimpleSelect[Q, R](
      expr: Q,
      exprPrefix: Option[Context => SqlStr],
      preserveAll: Boolean,
      from: Seq[Context.From],
      joins: Seq[Join],
      where: Seq[Db[_]],
      groupBy0: Option[GroupBy]
  )(implicit qr: Queryable.Row[Q, R])
      extends scalasql.query.SimpleSelect(
        expr,
        exprPrefix,
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
    protected override def selectRenderer(prevContext: Context) = {
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
