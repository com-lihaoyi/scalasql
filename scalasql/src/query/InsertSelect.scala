package scalasql.query

import scalasql.dialects.Dialect
import scalasql.renderer.SqlStr.{Renderable, SqlStringSyntax}
import scalasql.renderer.{Context, SqlStr}
import scalasql.{Column, Queryable, Table, TypeMapper}

/**
 * A SQL `INSERT SELECT` query
 */
trait InsertSelect[Q, C, R, R2] extends InsertReturnable[Q] with Query[Int]

object InsertSelect {
  class Impl[Q, C, R, R2](insert: Insert[Q, R], columns: C, select: Select[C, R2])(
      implicit dialect: Dialect
  ) extends InsertSelect[Q, C, R, R2] {
    import dialect.{dialectSelf => _, _}
    protected def expr = WithExpr.get(insert)

    def table = insert.table

    protected override def renderToSql(ctx: Context) =
      new Renderer(select, select.qr.walk(columns).map(_._2), ctx, Table.tableName(table.value))
        .render()

    protected override def queryIsExecuteUpdate = true

    protected def queryWalkExprs() = Nil

    protected override def queryIsSingleRow = true

    override protected def queryConstruct(args: Queryable.ResultSetIterator): Int =
      args.get(IntType)
  }

  class Renderer(
      select: Select[_, _],
      exprs: Seq[Expr[_]],
      prevContext: Context,
      tableName: String
  ) {

    implicit lazy val ctx: Context = prevContext.withExprNaming(Map()).withFromNaming(Map())

    lazy val columns = SqlStr.join(
      exprs
        .map(_.asInstanceOf[Column.ColumnExpr[_]])
        .map(c => SqlStr.raw(ctx.config.columnNameMapper(c.name))),
      SqlStr.commaSep
    )

    lazy val selectSql = Renderable.renderToSql(select).withCompleteQuery(false)

    lazy val tableNameStr = SqlStr.raw(ctx.config.tableNameMapper(tableName))
    def render() = sql"INSERT INTO $tableNameStr ($columns) $selectSql"
  }
}
