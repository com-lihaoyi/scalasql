package scalasql.query

import scalasql.core.SqlStr.{Renderable, SqlStringSyntax}
import scalasql.core.{Context, Queryable, SqlStr, WithSqlExpr}

/**
 * A query with a SQL `ON CONFLICT` clause, typically an `INSERT` or an `UPDATE`
 */
class OnConflict[Q, R](query: Query[R] with InsertReturnable[Q], expr: Q, table: TableRef) {
  def onConflictIgnore(c: (Q => Column[_])*) =
    new OnConflict.Ignore(query, c.map(_(expr)), table)
  def onConflictUpdate(c: (Q => Column[_])*)(c2: (Q => Column.Assignment[_])*) =
    new OnConflict.Update(query, c.map(_(expr)), c2.map(_(expr)), table)
}

object OnConflict {
  class Ignore[Q, R](
      query: Query[R] with InsertReturnable[Q],
      columns: Seq[Column[_]],
      val table: TableRef
  ) extends Query[R]
      with InsertReturnable[Q] {
    protected def expr = WithSqlExpr.get(query)
    protected def queryWalkLabels() = Query.walkLabels(query)
    protected def queryWalkExprs() = Query.walkSqlExprs(query)
    protected def queryIsSingleRow = Query.isSingleRow(query)
    protected def renderToSql(ctx: Context) = {
      val str = Renderable.toSql(query)(ctx)
      str + sql" ON CONFLICT (${SqlStr.join(columns.map(c => SqlStr.raw(c.name)), SqlStr.commaSep)}) DO NOTHING"
    }

    protected override def queryIsExecuteUpdate = true

    override protected def queryConstruct(args: Queryable.ResultSetIterator): R =
      Query.construct(query, args)
  }

  class Update[Q, R](
      query: Query[R] with InsertReturnable[Q],
      columns: Seq[Column[_]],
      updates: Seq[Column.Assignment[_]],
      val table: TableRef
  ) extends Query[R]
      with InsertReturnable[Q] {
    protected def expr = WithSqlExpr.get(query)

    protected def queryWalkLabels() = Query.walkLabels(query)
    protected def queryWalkExprs() = Query.walkSqlExprs(query)
    protected def queryIsSingleRow = Query.isSingleRow(query)
    protected def renderToSql(ctx: Context) = {
      implicit val implicitCtx = Context.compute(ctx, Nil, Some(table))
      val str = Renderable.toSql(query)
      val columnsStr = SqlStr.join(columns.map(c => SqlStr.raw(c.name)), SqlStr.commaSep)
      val updatesStr = SqlStr.join(
        updates.map { case assign => SqlStr.raw(assign.column.name) + sql" = ${assign.value}" },
        SqlStr.commaSep
      )
      str + sql" ON CONFLICT (${columnsStr}) DO UPDATE SET $updatesStr"
    }

    protected override def queryIsExecuteUpdate = true
    override protected def queryConstruct(args: Queryable.ResultSetIterator): R =
      Query.construct(query, args)
  }
}
