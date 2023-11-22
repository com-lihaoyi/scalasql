package scalasql.dialects

import scalasql.{Id, Queryable, Table, TypeMapper, dialects, operations}
import scalasql.query.{
  Aggregatable,
  AscDesc,
  CompoundSelect,
  Expr,
  From,
  GroupBy,
  Join,
  Nulls,
  OrderBy,
  Select
}
import scalasql.renderer.{Context, SqlStr}
import scalasql.renderer.SqlStr.SqlStringSyntax

trait SqliteDialect extends Dialect with ReturningDialect with OnConflictOps {
  def castParams = false

  override implicit def ExprOpsConv(v: Expr[_]): SqliteDialect.ExprOps =
    new SqliteDialect.ExprOps(v)

  override implicit def ExprStringOpsConv(v: Expr[String]): SqliteDialect.ExprStringOps =
    new SqliteDialect.ExprStringOps(v)

  override implicit def TableOpsConv[V[_[_]]](t: Table[V]): scalasql.operations.TableOps[V] =
    new SqliteDialect.TableOps(t)

  implicit def AggExprOpsConv[T](v: Aggregatable[Expr[T]]): operations.AggExprOps[T] =
    new SqliteDialect.AggExprOps(v)
}

object SqliteDialect extends SqliteDialect {
  class AggExprOps[T](v: Aggregatable[Expr[T]]) extends scalasql.operations.AggExprOps[T](v) {

    /** TRUE if all values in a set are TRUE */
    def mkString(sep: Expr[String] = null)(implicit tm: TypeMapper[T]): Expr[String] = {
      val sepRender = Option(sep).getOrElse(sql"''")
      v.queryExpr(expr => implicit ctx => sql"GROUP_CONCAT($expr || '', $sepRender)")
    }
  }
  class ExprOps(val v: Expr[_]) extends operations.ExprOps(v) {
    override def cast[V: TypeMapper]: Expr[V] = Expr { implicit ctx =>
      val s = implicitly[TypeMapper[V]] match {
        case TypeMapper.LocalDateType | TypeMapper.LocalDateTimeType | TypeMapper.InstantType =>
          "VARCHAR"
        case s => s.typeString
      }

      sql"CAST($v AS ${SqlStr.raw(s)})"
    }
  }
  class ExprStringOps(val v: Expr[String]) extends operations.ExprStringOps(v) with TrimOps {
    def indexOf(x: Expr[String]): Expr[Int] = Expr { implicit ctx => sql"INSTR($v, $x)" }
    def glob(x: Expr[String]): Expr[Int] = Expr { implicit ctx => sql"GLOB($v, $x)" }
  }

  class TableOps[V[_[_]]](t: Table[V]) extends scalasql.operations.TableOps[V](t) {

    protected override def joinableSelect: Select[V[Expr], V[Id]] = {
      val ref = t.tableRef
      new SimpleSelect(t.metadata.vExpr(ref).asInstanceOf[V[Expr]], None, Seq(ref), Nil, Nil, None)(
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
    )(implicit qr: Queryable.Row[Q, R]): scalasql.query.CompoundSelect[Q, R] = {
      new CompoundSelect(lhs, compoundOps, orderBy, limit, offset)
    }

    override def newSimpleSelect[Q, R](
        expr: Q,
        exprPrefix: Option[String],
        from: Seq[From],
        joins: Seq[Join],
        where: Seq[Expr[_]],
        groupBy0: Option[GroupBy]
    )(implicit qr: Queryable.Row[Q, R]): scalasql.query.SimpleSelect[Q, R] = {
      new SimpleSelect(expr, exprPrefix, from, joins, where, groupBy0)
    }
  }

  class SimpleSelect[Q, R](
      expr: Q,
      exprPrefix: Option[String],
      from: Seq[From],
      joins: Seq[Join],
      where: Seq[Expr[_]],
      groupBy0: Option[GroupBy]
  )(implicit qr: Queryable.Row[Q, R])
      extends scalasql.query.SimpleSelect(expr, exprPrefix, from, joins, where, groupBy0)
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
