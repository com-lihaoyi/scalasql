package scalasql.query

import scalasql.renderer.SqlStr.{Renderable, SqlStringSyntax}
import scalasql.renderer.{Context, SqlStr}
import scalasql.{Column, TypeMapper, Queryable}
import scalasql.utils.OptionPickler

/**
 * A SQL `INSERT SELECT` query
 */
trait InsertSelect[Q, C, R, R2] extends InsertReturnable[Q] with Query[Int]

object InsertSelect {
  class Impl[Q, C, R, R2](insert: Insert[Q, R], columns: C, select: Select[C, R2])
      extends InsertSelect[Q, C, R, R2] {
    protected def expr = WithExpr.get(insert)

    def table = insert.table

    protected override def renderToSql(ctx: Context) =
      new Renderer(select, select.qr.walk(columns).map(_._2), ctx, table.value.tableName).render()

    protected override def queryTypeMappers() = Seq(TypeMapper.IntType)

    protected override def queryIsExecuteUpdate = true

    protected def queryWalkExprs() = Nil

    protected override def queryIsSingleRow = true

    protected override def queryValueReader: OptionPickler.Reader[Int] = implicitly
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
      sql", "
    )

    lazy val selectSql = Renderable.renderToSql(select).withCompleteQuery(false)

    lazy val tableNameStr = SqlStr.raw(ctx.config.tableNameMapper(tableName))
    def render() = sql"INSERT INTO $tableNameStr ($columns) $selectSql"
  }
}
