package scalasql.dialects

import scalasql.dialects.MySqlDialect.CompoundSelectRenderer
import scalasql.core.{Aggregatable, Context, Db, DbApi, DialectTypeMappers, JoinNullable, Queryable, SqlStr, TypeMapper}
import scalasql.{Sc, dialects, operations}
import scalasql.query.{CompoundSelect, GroupBy, InsertColumns, InsertSelect, Join, Joinable, OrderBy, Query, Table}
import scalasql.core.SqlStr.SqlStringSyntax
import scalasql.operations.{BitwiseFunctionOps, ConcatOps, PadOps, TrimOps}

import java.sql.{JDBCType, PreparedStatement, ResultSet}

trait H2Dialect extends Dialect {

  protected def dialectCastParams = true

  override implicit def EnumType[T <: Enumeration#Value](
      implicit constructor: String => T
  ): TypeMapper[T] = new H2EnumType[T]
  class H2EnumType[T](implicit constructor: String => T) extends EnumType[T] {
    override def put(r: PreparedStatement, idx: Int, v: T): Unit = r.setString(idx, v.toString)
  }

  override implicit def DbStringOpsConv(v: Db[String]): H2Dialect.SqlStringOps =
    new H2Dialect.SqlStringOps(v)
  override implicit def DbNumericOpsConv[T: Numeric: TypeMapper](
      v: Db[T]
  ): H2Dialect.SqlNumericOps[T] = new H2Dialect.SqlNumericOps(v)

  override implicit def TableOpsConv[V[_[_]]](t: Table[V]): scalasql.dialects.TableOps[V] =
    new H2Dialect.TableOps(t)

  override implicit def DbApiQueryOpsConv(db: => DbApi): DbApiQueryOps = new DbApiQueryOps(this) {
    override def values[Q, R](ts: Seq[R])(implicit qr: Queryable.Row[Q, R]) =
      new H2Dialect.Values(ts)
  }

  implicit def DbAggOpsConv[T](v: Aggregatable[Db[T]]): operations.DbAggOps[T] =
    new H2Dialect.SqlAggOps(v)

  override implicit def DbApiOpsConv(db: => DbApi): H2Dialect.DbApiOps = new H2Dialect.DbApiOps(this)
}

object H2Dialect extends H2Dialect {
  class DbApiOps(dialect: DialectTypeMappers) extends scalasql.operations.DbApiOps(dialect) with ConcatOps
  class SqlAggOps[T](v: Aggregatable[Db[T]]) extends scalasql.operations.DbAggOps[T](v) {
    def mkString(sep: Db[String] = null)(implicit tm: TypeMapper[T]): Db[String] = {
      assert(
        sep == null,
        "H2 database dialect does not support mkString separator due to a bug (?) where " +
          "the separator is being treated as empty when a prepared statement placeholder is given"
      )
      val sepRender = Option(sep).getOrElse(sql"''")

      v.queryExpr(expr => implicit ctx => sql"LISTAGG($expr || '', $sepRender)")
    }
  }

  class SqlStringOps(protected val v: Db[String])
      extends operations.DbStringOps(v)
      with TrimOps
      with PadOps {
    def indexOf(x: Db[String]): Db[Int] = Db { implicit ctx => sql"INSTR($v, $x)" }
  }

  class SqlNumericOps[T: Numeric: TypeMapper](protected val v: Db[T])
      extends operations.DbNumericOps[T](v)
      with BitwiseFunctionOps[T]

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
      with Select[Q, R] {
    override def outerJoin[Q2, R2](other: Joinable[Q2, R2])(on: (Q, Q2) => Db[Boolean])(
        implicit joinQr: Queryable.Row[Q2, R2]
    ): scalasql.query.Select[(JoinNullable[Q], JoinNullable[Q2]), (Option[R], Option[R2])] = {
      leftJoin(other)(on)
        .map { case (l, r) => (JoinNullable(l), r) }
        .union(rightJoin(other)(on).map { case (l, r) =>
          (l, JoinNullable(r))
        })
    }
  }

  class CompoundSelect[Q, R](
      lhs: scalasql.query.SimpleSelect[Q, R],
      compoundOps: Seq[scalasql.query.CompoundSelect.Op[Q, R]],
      orderBy: Seq[OrderBy],
      limit: Option[Int],
      offset: Option[Int]
  )(implicit qr: Queryable.Row[Q, R])
      extends scalasql.query.CompoundSelect(lhs, compoundOps, orderBy, limit, offset)
      with Select[Q, R]

  class Values[Q, R](ts: Seq[R])(implicit qr: Queryable.Row[Q, R])
      extends scalasql.query.Values[Q, R](ts) {
    override protected def columnName(n: Int) = s"c${n + 1}"
  }
}
