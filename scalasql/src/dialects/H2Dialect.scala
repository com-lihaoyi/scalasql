package scalasql.dialects

import scalasql.dialects.MySqlDialect.CompoundSelectRenderer
import scalasql.core.{
  Aggregatable,
  JoinNullable,
  From,
  Column,
  DbApi,
  Queryable,
  Table,
  TypeMapper,
  Sql,
  SqlStr
}
import scalasql.{Id, dialects, operations}
import scalasql.query.{
  CompoundSelect,
  GroupBy,
  InsertColumns,
  InsertSelect,
  Join,
  Joinable,
  OrderBy,
  Query
}
import scalasql.core.Context
import scalasql.core.SqlStr.SqlStringSyntax

import java.sql.{JDBCType, PreparedStatement, ResultSet}

trait H2Dialect extends Dialect {

  protected def dialectCastParams = true

  override implicit def EnumType[T <: Enumeration#Value](
      implicit constructor: String => T
  ): TypeMapper[T] = new H2EnumType[T]
  class H2EnumType[T](implicit constructor: String => T) extends EnumType[T] {
    override def put(r: PreparedStatement, idx: Int, v: T): Unit = r.setString(idx, v.toString)
  }

  override implicit def ExprStringOpsConv(v: Sql[String]): H2Dialect.ExprStringOps =
    new H2Dialect.ExprStringOps(v)
  override implicit def ExprNumericOpsConv[T: Numeric: TypeMapper](
      v: Sql[T]
  ): H2Dialect.ExprNumericOps[T] = new H2Dialect.ExprNumericOps(v)

  override implicit def TableOpsConv[V[_[_]]](t: Table[V]): scalasql.dialects.TableOps[V] =
    new H2Dialect.TableOps(t)

  override implicit def DbApiOpsConv(db: => DbApi): DbApiOps = new DbApiOps(this) {
    override def values[Q, R](ts: Seq[R])(implicit qr: Queryable.Row[Q, R]) =
      new H2Dialect.Values(ts)
  }

  implicit def AggExprOpsConv[T](v: Aggregatable[Sql[T]]): operations.AggExprOps[T] =
    new H2Dialect.AggExprOps(v)
}

object H2Dialect extends H2Dialect {
  class AggExprOps[T](v: Aggregatable[Sql[T]]) extends scalasql.operations.AggExprOps[T](v) {
    def mkString(sep: Sql[String] = null)(implicit tm: TypeMapper[T]): Sql[String] = {
      assert(
        sep == null,
        "H2 database dialect does not support mkString separator due to a bug (?) where " +
          "the separator is being treated as empty when a prepared statement placeholder is given"
      )
      val sepRender = Option(sep).getOrElse(sql"''")

      v.queryExpr(expr => implicit ctx => sql"LISTAGG($expr || '', $sepRender)")
    }
  }

  class ExprStringOps(protected val v: Sql[String])
      extends operations.ExprStringOps(v)
      with TrimOps
      with PadOps {
    def indexOf(x: Sql[String]): Sql[Int] = Sql { implicit ctx => sql"INSTR($v, $x)" }
  }

  class ExprNumericOps[T: Numeric: TypeMapper](protected val v: Sql[T])
      extends operations.ExprNumericOps[T](v)
      with BitwiseFunctionOps[T]

  class TableOps[V[_[_]]](t: Table[V]) extends scalasql.dialects.TableOps[V](t) {
    protected override def joinableSelect: Select[V[Sql], V[Id]] = {
      val ref = Table.tableRef(t)
      new SimpleSelect(
        Table.tableMetadata(t).vExpr(ref, dialectSelf).asInstanceOf[V[Sql]],
        None,
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
        dialect: scalasql.core.DialectBase
    ): scalasql.query.CompoundSelect[Q, R] = {
      new CompoundSelect(lhs, compoundOps, orderBy, limit, offset)
    }

    override def newSimpleSelect[Q, R](
        expr: Q,
        exprPrefix: Option[Context => SqlStr],
        from: Seq[From],
        joins: Seq[Join],
        where: Seq[Sql[_]],
        groupBy0: Option[GroupBy]
    )(
        implicit qr: Queryable.Row[Q, R],
        dialect: scalasql.core.DialectBase
    ): scalasql.query.SimpleSelect[Q, R] = {
      new SimpleSelect(expr, exprPrefix, from, joins, where, groupBy0)
    }
  }

  class SimpleSelect[Q, R](
      expr: Q,
      exprPrefix: Option[Context => SqlStr],
      from: Seq[From],
      joins: Seq[Join],
      where: Seq[Sql[_]],
      groupBy0: Option[GroupBy]
  )(implicit qr: Queryable.Row[Q, R])
      extends scalasql.query.SimpleSelect(expr, exprPrefix, from, joins, where, groupBy0)
      with Select[Q, R] {
    override def outerJoin[Q2, R2](other: Joinable[Q2, R2])(on: (Q, Q2) => Sql[Boolean])(
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
