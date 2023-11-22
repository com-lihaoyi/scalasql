package scalasql.query

import scalasql.renderer.SqlStr.{Renderable, SqlStringSyntax}
import scalasql.{Column, TypeMapper}
import scalasql.renderer.{Context, SqlStr}

/**
 * A query with a SQL `ON CONFLICT` clause, typically an `INSERT` or an `UPDATE`
 */
class OnConflict[Q, R](query: Query[R] with InsertReturnable[Q], expr: Q, table: TableRef) {
  def onConflictIgnore(c: (Q => Column.ColumnExpr[_])*) =
    new OnConflict.Ignore(query, c.map(_(expr)), table)
  def onConflictUpdate(c: (Q => Column.ColumnExpr[_])*)(c2: (Q => Column.Assignment[_])*) =
    new OnConflict.Update(query, c.map(_(expr)), c2.map(_(expr)), table)
}

object OnConflict {
  class Ignore[Q, R](
      query: Query[R] with InsertReturnable[Q],
      columns: Seq[Column.ColumnExpr[_]],
      val table: TableRef
  ) extends Query[R]
      with InsertReturnable[Q] {
    protected def expr = WithExpr.get(query)
    protected def queryWalkExprs() = Query.queryWalkExprs(query)
    protected def queryIsSingleRow = Query.queryIsSingleRow(query)
    protected def renderToSql(ctx: Context) = {
      val str = Renderable.renderToSql(query)(ctx)
      str + sql" ON CONFLICT (${SqlStr.join(columns.map(c => SqlStr.raw(c.name)), sql", ")}) DO NOTHING"
    }

    protected def queryTypeMappers() = Query.queryTypeMappers(query)

    protected override def queryIsExecuteUpdate = true

    protected def queryValueReader = Query.queryValueReader(query)

  }

  class Update[Q, R](
      query: Query[R] with InsertReturnable[Q],
      columns: Seq[Column.ColumnExpr[_]],
      updates: Seq[Column.Assignment[_]],
      val table: TableRef
  ) extends Query[R]
      with InsertReturnable[Q] {
    protected def expr = WithExpr.get(query)
    protected def queryWalkExprs() = Query.queryWalkExprs(query)
    protected def queryIsSingleRow = Query.queryIsSingleRow(query)
    protected def renderToSql(ctx: Context) = {
      implicit val implicitCtx = Context.compute(ctx, Nil, Some(table))
      val str = Renderable.renderToSql(query)
      val columnsStr = SqlStr.join(columns.map(c => SqlStr.raw(c.name)), sql", ")
      val updatesStr = SqlStr.join(
        updates.map { case assign => SqlStr.raw(assign.column.name) + sql" = ${assign.value}" },
        sql", "
      )
      str + sql" ON CONFLICT (${columnsStr}) DO UPDATE SET $updatesStr"
    }

    protected def queryTypeMappers() = Query.queryTypeMappers(query)
    protected override def queryIsExecuteUpdate = true
    protected def queryValueReader = Query.queryValueReader(query)
  }
}
