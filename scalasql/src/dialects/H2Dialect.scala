package scalasql.dialects

import scalasql.core.{
  Aggregatable,
  Context,
  Expr,
  DbApi,
  DialectTypeMappers,
  JoinNullable,
  Queryable,
  SqlStr,
  TypeMapper
}
import scalasql.{Sc, operations}
import scalasql.query.{CompoundSelect, GroupBy, Join, Joinable, OrderBy, Table, Values}
import scalasql.core.SqlStr.SqlStringSyntax
import scalasql.operations.{
  BitwiseFunctionOps,
  ConcatOps,
  HyperbolicMathOps,
  MathOps,
  PadOps,
  TrimOps
}

import java.sql.PreparedStatement

trait H2Dialect extends Dialect {

  protected def dialectCastParams = true

  override implicit def EnumType[T <: Enumeration#Value](
      implicit constructor: String => T
  ): TypeMapper[T] = new H2EnumType[T]
  class H2EnumType[T](implicit constructor: String => T) extends EnumType[T] {
    override def put(r: PreparedStatement, idx: Int, v: T): Unit = r.setString(idx, v.toString)
  }

  override implicit def ExprStringOpsConv(v: Expr[String]): H2Dialect.ExprStringOps[String] =
    new H2Dialect.ExprStringOps(v)

  override implicit def ExprBlobOpsConv(
      v: Expr[geny.Bytes]
  ): H2Dialect.ExprStringLikeOps[geny.Bytes] =
    new H2Dialect.ExprStringLikeOps(v)

  override implicit def ExprNumericOpsConv[T: Numeric: TypeMapper](
      v: Expr[T]
  ): H2Dialect.ExprNumericOps[T] = new H2Dialect.ExprNumericOps(v)

  override implicit def TableOpsConv[V[_[_]]](t: Table[V]): scalasql.dialects.TableOps[V] =
    new H2Dialect.TableOps(t)

  override implicit def DbApiQueryOpsConv(db: => DbApi): DbApiQueryOps = new DbApiQueryOps(this) {
    override def values[Q, R](ts: Seq[R])(implicit qr: Queryable.Row[Q, R]): Values[Q, R] =
      new H2Dialect.Values(ts)
  }

  implicit def ExprAggOpsConv[T](v: Aggregatable[Expr[T]]): operations.ExprAggOps[T] =
    new H2Dialect.ExprAggOps(v)

  override implicit def DbApiOpsConv(db: => DbApi): H2Dialect.DbApiOps =
    new H2Dialect.DbApiOps(this)

}

object H2Dialect extends H2Dialect {
  class DbApiOps(dialect: DialectTypeMappers)
      extends scalasql.operations.DbApiOps(dialect)
      with ConcatOps
      with MathOps
      with HyperbolicMathOps

  class ExprAggOps[T](v: Aggregatable[Expr[T]]) extends scalasql.operations.ExprAggOps[T](v) {
    def mkString(sep: Expr[String] = null)(implicit tm: TypeMapper[T]): Expr[String] = {
      assert(
        sep == null,
        "H2 database dialect does not support mkString separator due to a bug (?) where " +
          "the separator is being treated as empty when a prepared statement placeholder is given"
      )
      val sepRender = Option(sep).getOrElse(sql"''")

      v.aggregateExpr(expr => implicit ctx => sql"LISTAGG($expr || '', $sepRender)")
    }
  }

  class ExprStringOps[T](v: Expr[T]) extends ExprStringLikeOps(v) with operations.ExprStringOps[T]
  class ExprStringLikeOps[T](protected val v: Expr[T])
      extends operations.ExprStringLikeOps(v)
      with TrimOps
      with PadOps {
    def indexOf(x: Expr[T]): Expr[Int] = Expr { implicit ctx => sql"INSTR($v, $x)" }
  }

  class ExprNumericOps[T: Numeric: TypeMapper](protected val v: Expr[T])
      extends operations.ExprNumericOps[T](v)
      with BitwiseFunctionOps[T] {
    def power(y: Expr[T]): Expr[T] = Expr { implicit ctx => sql"POWER($v, $y)" }
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

  class Values[Q, R](ts: Seq[R])(implicit qr: Queryable.Row[Q, R])
      extends scalasql.query.Values[Q, R](ts) {
    override protected def columnName(n: Int) = s"c${n + 1}"
  }
}
