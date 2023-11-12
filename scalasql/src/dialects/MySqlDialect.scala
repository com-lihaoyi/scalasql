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
  JoinNullable,
  Nulls,
  OrderBy,
  Query,
  TableRef,
  Update,
  WithExpr
}
import scalasql.renderer.SqlStr.{Renderable, SqlStringSyntax, optSeq}
import scalasql.renderer.{Context, ExprsToSql, JoinsToSql, SqlStr}
import scalasql.utils.OptionPickler

trait MySqlDialect extends Dialect {
  def defaultQueryableSuffix = ""
  def castParams = false

  override implicit def ExprOpsConv(v: Expr[_]): MySqlDialect.ExprOps =
    new MySqlDialect.ExprOps(v)

  override implicit def ExprStringOpsConv(v: Expr[String]): MySqlDialect.ExprStringOps =
    new MySqlDialect.ExprStringOps(v)

  override implicit def TableOpsConv[V[_[_]]](t: Table[V]): scalasql.operations.TableOps[V] =
    new MySqlDialect.TableOps(t)

  implicit def OnConflictableUpdate[Q, R](
      query: InsertValues[Q, R]
  ): MySqlDialect.OnConflictable[Q, Int] =
    new MySqlDialect.OnConflictable[Q, Int](query, WithExpr.get(query), query.table)
}

object MySqlDialect extends MySqlDialect {
  class ExprOps(val v: Expr[_]) extends operations.ExprOps(v) {
    override def cast[V: MappedType]: Expr[V] = Expr { implicit ctx =>
      val s = implicitly[MappedType[V]] match{
        case MappedType.ByteType | MappedType.ShortType | MappedType.IntType | MappedType.LongType => "SIGNED"
        case MappedType.StringType => "BINARY"
        case s => s.typeString
      }

      sql"CAST($v AS ${SqlStr.raw(s)})"
    }
  }
  class ExprStringOps(val v: Expr[String]) extends operations.ExprStringOps(v) with PadOps {
    override def +(x: Expr[String]): Expr[String] = Expr { implicit ctx => sql"CONCAT($v, $x)" }

    def indexOf(x: Expr[String]): Expr[Int] = Expr { implicit ctx => sql"POSITION($x IN $v)" }
    def reverse: Expr[String] = Expr { implicit ctx => sql"REVERSE($v)" }
  }

  class TableOps[V[_[_]]](t: Table[V]) extends scalasql.operations.TableOps[V](t) {
    override def update(
        filter: V[Column.ColumnExpr] => Expr[Boolean]
    ): Update[V[Column.ColumnExpr], V[Id]] = {
      val ref = t.tableRef
      new Update(t.metadata.vExpr(ref), ref, Nil, Nil, Seq(filter(t.metadata.vExpr(ref))))(
        t.containerQr
      )
    }

    protected override def joinableSelect: Select[V[Expr], V[Id]] = {
      val ref = t.tableRef
      new SimpleSelect(t.metadata.vExpr(ref).asInstanceOf[V[Expr]], None, Seq(ref), Nil, Nil, None)(
        t.containerQr
      )
    }
  }

  class Update[Q, R](
      expr: Q,
      table: TableRef,
      set0: Seq[Column.Assignment[_]],
      joins: Seq[Join],
      where: Seq[Expr[_]]
  )(implicit qr: Queryable.Row[Q, R])
      extends scalasql.query.Update.Impl[Q, R](expr, table, set0, joins, where) {

    protected override def copy[Q, R](
        expr: Q = this.expr,
        table: TableRef = this.table,
        set0: Seq[Column.Assignment[_]] = this.set0,
        joins: Seq[Join] = this.joins,
        where: Seq[Expr[_]] = this.where
    )(implicit qr: Queryable.Row[Q, R]) = new Update(expr, table, set0, joins, where)

    override def renderToSql(implicit ctx: Context): (SqlStr, Seq[MappedType[_]]) = {
      toSqlQuery0(this, ctx)
    }

    def toSqlQuery0[Q, R](
        q: Update.Impl[Q, R],
        prevContext: Context
    ): (SqlStr, Seq[MappedType[_]]) = {
      new UpdateRenderer(q.joins, q.table, q.set0, q.where, prevContext).render()
    }

  }

  class UpdateRenderer(
      joins0: Seq[Join],
      table: TableRef,
      set0: Seq[Column.Assignment[_]],
      where0: Seq[Expr[_]],
      prevContext: Context
  ) extends scalasql.query.Update.Renderer(joins0, table, set0, where0, prevContext) {
    import computed.implicitCtx
    override lazy val updateList = set0.map { case assign =>
      val colStr = SqlStr.raw(prevContext.config.columnNameMapper(assign.column.name))
      sql"$tableName.$colStr = ${assign.value}"
    }

    override lazy val where = ExprsToSql.booleanExprs(sql" WHERE ", where0)
    override lazy val joinOns = joins0
      .map(_.from.map(_.on.map(t => SqlStr.flatten(Renderable.renderToSql(t)._1))))

    override lazy val joins = optSeq(joins0)(
      JoinsToSql.joinsToSqlStr(_, computed.fromSelectables, Some(liveExprs), joinOns)
    )
    override def render() = (sql"UPDATE $tableName" + joins + sql" SET " + sets + where, Nil)
  }

  class OnConflictable[Q, R](val query: Query[R], expr: Q, table: TableRef) {

    def onConflictUpdate(c2: Q => Column.Assignment[_]*): OnConflictUpdate[Q, R] =
      new OnConflictUpdate(this, c2.map(_(expr)), table)
  }

  class OnConflictUpdate[Q, R](
      insert: OnConflictable[Q, R],
      updates: Seq[Column.Assignment[_]],
      table: TableRef
  ) extends Query[R] {

    override def queryIsExecuteUpdate = true
    protected def queryWalkExprs() = Query.getWalkExprs(insert.query)

    protected def queryIsSingleRow = Query.getIsSingleRow(insert.query)

    protected def queryValueReader = Query.getValueReader(insert.query)

    protected def renderToSql(implicit ctx: Context): (SqlStr, Seq[MappedType[_]]) = toSqlQuery0(
      ctx
    )
    def toSqlQuery0(ctx: Context): (SqlStr, Seq[MappedType[_]]) = {
      val computed = Context.compute(ctx, Nil, Some(table))
      import computed.implicitCtx
      val (str, mapped) = Renderable.renderToSql(insert.query)
      val updatesStr = SqlStr.join(
        updates.map { case assign => SqlStr.raw(assign.column.name) + sql" = ${assign.value}" },
        sql", "
      )
      (str + sql" ON DUPLICATE KEY UPDATE $updatesStr", mapped)
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
      with Select[Q, R] {
    protected override def getRenderer(prevContext: Context) =
      new CompoundSelectRenderer(this, prevContext)
  }

  class CompoundSelectRenderer[Q, R](
      query: scalasql.query.CompoundSelect[Q, R],
      prevContext: Context
  ) extends scalasql.query.CompoundSelect.Renderer(query, prevContext) {

    override lazy val limitOpt = SqlStr
      .flatten(CompoundSelectRendererForceLimit.limitToSqlStr(query.limit, query.offset))

    override def orderToSqlStr(newCtx: Context) = {
      SqlStr.optSeq(query.orderBy) { orderBys =>
        val orderStr = SqlStr.join(
          orderBys.map { orderBy =>
            val exprStr = Renderable.renderToSql(orderBy.expr)(newCtx)._1

            (orderBy.ascDesc, orderBy.nulls) match {
              case (Some(AscDesc.Asc), None | Some(Nulls.First)) => sql"$exprStr ASC"
              case (Some(AscDesc.Desc), Some(Nulls.First)) =>
                sql"$exprStr IS NULL DESC, $exprStr DESC"
              case (Some(AscDesc.Asc), Some(Nulls.Last)) => sql"$exprStr IS NULL ASC, $exprStr ASC"
              case (Some(AscDesc.Desc), None | Some(Nulls.Last)) => sql"$exprStr DESC"
              case (None, None) => exprStr
              case (None, Some(Nulls.First)) => sql"$exprStr IS NULL DESC, $exprStr"
              case (None, Some(Nulls.Last)) => sql"$exprStr IS NULL ASC, $exprStr"
            }
          },
          sql", "
        )

        sql" ORDER BY $orderStr"
      }
    }
  }

}
