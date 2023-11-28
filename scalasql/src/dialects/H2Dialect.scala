package scalasql.dialects

import scalasql.dialects.MySqlDialect.CompoundSelectRenderer
import scalasql.operations.DbApiOps
import scalasql.{Column, DbApi, Id, Queryable, Table, TypeMapper, dialects, operations}
import scalasql.query.{
  Aggregatable,
  CompoundSelect,
  Expr,
  From,
  GroupBy,
  InsertSelect,
  InsertColumns,
  Join,
  JoinNullable,
  Joinable,
  OrderBy,
  Query
}
import scalasql.renderer.{Context, SqlStr}
import scalasql.renderer.SqlStr.SqlStringSyntax

trait H2Dialect extends Dialect {

  protected def dialectCastParams = true

  override implicit def ExprStringOpsConv(v: Expr[String]): H2Dialect.ExprStringOps =
    new H2Dialect.ExprStringOps(v)
  override implicit def ExprNumericOpsConv[T: Numeric: TypeMapper](
      v: Expr[T]
  ): H2Dialect.ExprNumericOps[T] = new H2Dialect.ExprNumericOps(v)

  override implicit def TableOpsConv[V[_[_]]](t: Table[V]): scalasql.operations.TableOps[V] =
    new H2Dialect.TableOps(t)

  override implicit def DbApiOpsConv(db: => DbApi): DbApiOps = new DbApiOps(this) {
    override def values[T: TypeMapper](ts: Seq[T]) = new H2Dialect.Values(ts)
  }

  implicit def AggExprOpsConv[T](v: Aggregatable[Expr[T]]): operations.AggExprOps[T] =
    new H2Dialect.AggExprOps(v)
}

object H2Dialect extends H2Dialect {
  class AggExprOps[T](v: Aggregatable[Expr[T]]) extends scalasql.operations.AggExprOps[T](v) {
    def mkString(sep: Expr[String] = null)(implicit tm: TypeMapper[T]): Expr[String] = {
      assert(
        sep == null,
        "H2 database dialect does not support mkString separator due to a bug (?) where " +
          "the separator is being treated as empty when a prepared statement placeholder is given"
      )
      val sepRender = Option(sep).getOrElse(sql"''")

      v.queryExpr(expr => implicit ctx => sql"LISTAGG($expr || '', $sepRender)")
    }
  }

  class ExprStringOps(protected val v: Expr[String])
      extends operations.ExprStringOps(v)
      with TrimOps
      with PadOps {
    def indexOf(x: Expr[String]): Expr[Int] = Expr { implicit ctx => sql"INSTR($v, $x)" }
  }

  class ExprNumericOps[T: Numeric: TypeMapper](protected val v: Expr[T])
      extends operations.ExprNumericOps[T](v)
      with BitwiseFunctionOps[T]

  class TableOps[V[_[_]]](t: Table[V]) extends scalasql.operations.TableOps[V](t) {
    protected override def joinableSelect: Select[V[Expr], V[Id]] = {
      val ref = Table.tableRef(t)
      new SimpleSelect(
        Table.tableMetadata(t).vExpr(ref, dialectSelf).asInstanceOf[V[Expr]],
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
    )(implicit qr: Queryable.Row[Q, R], dialect: Dialect): scalasql.query.CompoundSelect[Q, R] = {
      new CompoundSelect(lhs, compoundOps, orderBy, limit, offset)
    }

    override def newSimpleSelect[Q, R](
        expr: Q,
        exprPrefix: Option[Context => SqlStr],
        from: Seq[From],
        joins: Seq[Join],
        where: Seq[Expr[_]],
        groupBy0: Option[GroupBy]
    )(implicit qr: Queryable.Row[Q, R], dialect: Dialect): scalasql.query.SimpleSelect[Q, R] = {
      new SimpleSelect(expr, exprPrefix, from, joins, where, groupBy0)
    }
  }

  class SimpleSelect[Q, R](
      expr: Q,
      exprPrefix: Option[Context => SqlStr],
      from: Seq[From],
      joins: Seq[Join],
      where: Seq[Expr[_]],
      groupBy0: Option[GroupBy]
  )(implicit qr: Queryable.Row[Q, R])
      extends scalasql.query.SimpleSelect(expr, exprPrefix, from, joins, where, groupBy0)
      with Select[Q, R] {
    override def outerJoin[Q2, R2](other: Joinable[Q2, R2])(on: (Q, Q2) => Expr[Boolean])(
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

  class Values[T: TypeMapper](ts: Seq[T]) extends scalasql.query.Values[T](ts) {
    override protected def columnName = "c1"
  }
}
