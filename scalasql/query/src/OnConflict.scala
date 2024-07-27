package scalasql.query

import scalasql.core.SqlStr.{Renderable, SqlStringSyntax}
import scalasql.core.{Context, Queryable, SqlStr, WithSqlExpr}

/**
 * A query with a SQL `ON CONFLICT` clause, typically an `INSERT` or an `UPDATE`
 */
class OnConflict[Q, R](query: Query[R] & Returning.InsertBase[Q], expr: Q, table: TableRef) {
  def onConflictIgnore(c: (Q => Column[?])*) =
    new OnConflict.Ignore(query, c.map(_(expr)), table)
  def onConflictUpdate(c: (Q => Column[?])*)(c2: (Q => Column.Assignment[?])*) =
    new OnConflict.Update(query, c.map(_(expr)), c2.map(_(expr)), table)
}

object OnConflict {
  class Ignore[Q, R](
      protected val query: Query[R] & Returning.InsertBase[Q],
      columns: Seq[Column[?]],
      val table: TableRef
  ) extends Query.DelegateQuery[R]
      with Returning.InsertBase[Q] {
    protected def expr = WithSqlExpr.get(query)
    private[scalasql] def renderSql(ctx: Context) = {
      val str = Renderable.renderSql(query)(ctx)
      val columnsStr = SqlStr.join(
        columns.map(c => SqlStr.raw(ctx.config.columnNameMapper(c.name))),
        SqlStr.commaSep
      )
      str + sql" ON CONFLICT ($columnsStr) DO NOTHING"
    }

    protected override def queryIsExecuteUpdate = true

    override protected def queryConstruct(args: Queryable.ResultSetIterator): R =
      Query.construct(query, args)
  }

  class Update[Q, R](
      protected val query: Query[R] & Returning.InsertBase[Q],
      columns: Seq[Column[?]],
      updates: Seq[Column.Assignment[?]],
      val table: TableRef
  ) extends Query.DelegateQuery[R]
      with Returning.InsertBase[Q] {
    protected def expr = WithSqlExpr.get(query)
    private[scalasql] def renderSql(ctx: Context) = {
      implicit val implicitCtx = Context.compute(ctx, Nil, Some(table))
      val str = Renderable.renderSql(query)
      val columnsStr = SqlStr.join(
        columns.map(c => SqlStr.raw(ctx.config.columnNameMapper(c.name))),
        SqlStr.commaSep
      )
      val updatesStr = SqlStr.join(
        updates.map { case assign =>
          SqlStr.raw(ctx.config.columnNameMapper(assign.column.name)) + sql" = ${assign.value}"
        },
        SqlStr.commaSep
      )
      str + sql" ON CONFLICT (${columnsStr}) DO UPDATE SET $updatesStr"
    }

    protected override def queryIsExecuteUpdate = true
    override protected def queryConstruct(args: Queryable.ResultSetIterator): R =
      Query.construct(query, args)
  }
}
