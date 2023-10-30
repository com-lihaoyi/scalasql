package scalasql.dialects

import scalasql._
import scalasql.query.{
  AscDesc,
  CompoundSelect,
  Expr,
  From,
  GroupBy,
  InsertValues,
  Join,
  Joinable,
  Nulls,
  OrderBy,
  Query,
  TableRef,
  Update
}
import scalasql.renderer.SqlStr.{SqlStringSyntax, optSeq}
import scalasql.renderer.{Context, SelectToSql, SqlStr}
import scalasql.utils.OptionPickler

trait MySqlDialect extends Dialect {
  def defaultQueryableSuffix = ""
  def castParams = false

  override implicit def ExprStringOpsConv(v: Expr[String]): MySqlDialect.ExprStringOps =
    new MySqlDialect.ExprStringOps(v)

  override implicit def TableOpsConv[V[_[_]]](t: Table[V]): scalasql.operations.TableOps[V] =
    new MySqlDialect.TableOps(t)

  implicit def OnConflictableUpdate[Q, R](query: InsertValues[Q, R]) =
    new MySqlDialect.OnConflictable[Q, Int](query, query.expr, query.table)
}

object MySqlDialect extends MySqlDialect {
  class ExprStringOps(val v: Expr[String]) extends operations.ExprStringOps(v) with PadOps {
    override def +(x: Expr[String]): Expr[String] = Expr { implicit ctx => sql"CONCAT($v, $x)" }

    def indexOf(x: Expr[String]): Expr[Int] = Expr { implicit ctx => sql"POSITION($x IN $v)" }
    def reverse: Expr[String] = Expr { implicit ctx => sql"REVERSE($v)" }
  }

  class TableOps[V[_[_]]](t: Table[V]) extends scalasql.operations.TableOps[V](t) {
    override def update: Update[V[Column.ColumnExpr], V[Id]] = {
      val ref = t.tableRef
      new Update(t.metadata.vExpr(ref), ref, Nil, Nil, Nil)(t.containerQr)
    }

    override def select: Select[V[Expr], V[Id]] = {
      val ref = t.tableRef
      new SimpleSelect(t.metadata.vExpr(ref).asInstanceOf[V[Expr]], None, Seq(ref), Nil, Nil, None)(
        t.containerQr
      )
    }
  }

  class Update[Q, R](
      expr: Q,
      table: TableRef,
      set0: Seq[(Column.ColumnExpr[_], Expr[_])],
      joins: Seq[Join],
      where: Seq[Expr[_]]
  )(implicit qr: Queryable[Q, R])
      extends scalasql.query.Update.Impl[Q, R](expr, table, set0, joins, where) {

    override def copy[Q, R](
        expr: Q = this.expr,
        table: TableRef = this.table,
        set0: Seq[(Column.ColumnExpr[_], Expr[_])] = this.set0,
        joins: Seq[Join] = this.joins,
        where: Seq[Expr[_]] = this.where
    )(implicit qr: Queryable[Q, R]) = new Update(expr, table, set0, joins, where)

    override def toSqlQuery(implicit ctx: Context): (SqlStr, Seq[MappedType[_]]) = {
      toSqlQuery0(this, ctx)
    }

    def toSqlQuery0[Q, R](
        q: Update.Impl[Q, R],
        prevContext: Context
    ): (SqlStr, Seq[MappedType[_]]) = {
      val computed = Context
        .compute(prevContext, q.joins.flatMap(_.from).map(_.from), Some(q.table))
      import computed.implicitCtx

      val tableName = SqlStr.raw(prevContext.config.tableNameMapper(q.table.value.tableName))
      val updateList = q.set0.map { case (k, v) =>
        val colStr = SqlStr.raw(prevContext.config.columnNameMapper(k.name))
        sql"$tableName.$colStr = $v"
      }
      val sets = SqlStr.join(updateList, sql", ")

      val where = SqlStr.optSeq(q.where) { where =>
        sql" WHERE " + SqlStr.join(where.map(_.toSqlQuery._1), sql" AND ")
      }

      val joins = optSeq(q.joins)(SelectToSql.joinsToSqlStr(_, computed.fromSelectables))

      (sql"UPDATE $tableName" + joins + sql" SET " + sets + where, Nil)
    }

  }

  class OnConflictable[Q, R](val query: Query[R], expr: Q, table: TableRef) {

    def onConflictUpdate(c2: Q => (Column.ColumnExpr[_], Expr[_])*): OnConflictUpdate[Q, R] =
      new OnConflictUpdate(this, c2.map(_(expr)), table)
  }

  class OnConflictUpdate[Q, R](
      insert: OnConflictable[Q, R],
      updates: Seq[(Column.ColumnExpr[_], Expr[_])],
      table: TableRef
  ) extends Query[R] {

    override def isExecuteUpdate = true
    def walk() = insert.query.walk()

    def singleRow = insert.query.singleRow

    def valueReader = insert.query.valueReader

    def toSqlQuery(implicit ctx: Context): (SqlStr, Seq[MappedType[_]]) = toSqlQuery0(ctx)
    def toSqlQuery0(ctx: Context): (SqlStr, Seq[MappedType[_]]) = {
      val computed = Context.compute(ctx, Nil, Some(table))
      import computed.implicitCtx
      val (str, mapped) = insert.query.toSqlQuery
      val updatesStr = SqlStr
        .join(updates.map { case (c, e) => SqlStr.raw(c.name) + sql" = $e" }, sql", ")
      (str + sql" ON DUPLICATE KEY UPDATE $updatesStr", mapped)
    }
  }

  trait Select[Q, R] extends scalasql.query.Select[Q, R] {
    override def newCompoundSelect[Q, R](
        lhs: scalasql.query.SimpleSelect[Q, R],
        compoundOps: Seq[CompoundSelect.Op[Q, R]],
        orderBy: Option[OrderBy],
        limit: Option[Int],
        offset: Option[Int]
    )(implicit qr: Queryable[Q, R]): scalasql.query.CompoundSelect[Q, R] = {
      new CompoundSelect(lhs, compoundOps, orderBy, limit, offset)
    }

    override def newSimpleSelect[Q, R](
        expr: Q,
        exprPrefix: Option[String],
        from: Seq[From],
        joins: Seq[Join],
        where: Seq[Expr[_]],
        groupBy0: Option[GroupBy]
    )(implicit qr: Queryable[Q, R]): scalasql.query.SimpleSelect[Q, R] = {
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
  )(implicit qr: Queryable[Q, R])
      extends scalasql.query.SimpleSelect(expr, exprPrefix, from, joins, where, groupBy0)
      with Select[Q, R]

  class CompoundSelect[Q, R](
      lhs: scalasql.query.SimpleSelect[Q, R],
      compoundOps: Seq[CompoundSelect.Op[Q, R]],
      orderBy: Option[OrderBy],
      limit: Option[Int],
      offset: Option[Int]
  )(implicit qr: Queryable[Q, R])
      extends scalasql.query.CompoundSelect(lhs, compoundOps, orderBy, limit, offset)
      with Select[Q, R] {
    override def toSqlQuery0(prevContext: Context) = {
      new CompoundSelectRenderer(this, prevContext).toSqlStr()
    }
  }

  class CompoundSelectRenderer[Q, R](
      query: scalasql.query.CompoundSelect[Q, R],
      prevContext: Context
  ) extends scalasql.query.CompoundSelect.Renderer(query, prevContext) {

    override def limitOffsetToSqlStr = CompoundSelectRendererForceLimit
      .limitOffsetToSqlStr(query.limit, query.offset)

    override def orderToToSqlStr[R, Q](newCtx: Context) = {
      SqlStr.opt(query.orderBy) { orderBy =>
        val exprStr = orderBy.expr.toSqlQuery(newCtx)._1
        val str = (orderBy.ascDesc, orderBy.nulls) match {
          case (Some(AscDesc.Asc), None | Some(Nulls.First)) => sql"$exprStr ASC"
          case (Some(AscDesc.Desc), Some(Nulls.First)) => sql"$exprStr IS NULL DESC, $exprStr DESC"
          case (Some(AscDesc.Asc), Some(Nulls.Last)) => sql"$exprStr IS NULL ASC, $exprStr ASC"
          case (Some(AscDesc.Desc), None | Some(Nulls.Last)) => sql"$exprStr DESC"
          case (None, None) => exprStr
          case (None, Some(Nulls.First)) => sql"$exprStr IS NULL DESC, $exprStr"
          case (None, Some(Nulls.Last)) => sql"$exprStr IS NULL ASC, $exprStr"
        }

        sql" ORDER BY $str"
      }
    }
  }

}
