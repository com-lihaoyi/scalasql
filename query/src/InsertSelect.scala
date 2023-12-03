package scalasql.query

import scalasql.core.{
  Column,
  Context,
  DialectBase,
  Queryable,
  Sql,
  SqlStr,
  Table,
  TypeMapper,
  WithExpr
}
import scalasql.core.SqlStr.{Renderable, SqlStringSyntax}

/**
 * A SQL `INSERT SELECT` query
 */
trait InsertSelect[V[_[_]], C, R, R2] extends InsertReturnable[V[Sql]] with Query[Int]

object InsertSelect {
  class Impl[V[_[_]], C, R, R2](insert: Insert[V, R], columns: C, select: Select[C, R2])(
      implicit dialect: DialectBase
  ) extends InsertSelect[V, C, R, R2] {
    import dialect.{dialectSelf => _, _}
    protected def expr = WithExpr.get(insert).asInstanceOf[V[Sql]]

    def table = insert.table

    protected override def renderToSql(ctx: Context) =
      new Renderer(select, select.qr.walkExprs(columns), ctx, Table.name(table.value))
        .render()

    protected override def queryIsExecuteUpdate = true

    protected def queryWalkLabels() = Nil

    protected def queryWalkExprs() = Nil

    protected override def queryIsSingleRow = true

    override protected def queryConstruct(args: Queryable.ResultSetIterator): Int =
      args.get(IntType)
  }

  class Renderer(
      select: Select[_, _],
      exprs: Seq[Sql[_]],
      prevContext: Context,
      tableName: String
  ) {

    implicit lazy val ctx: Context = prevContext.withExprNaming(Map()).withFromNaming(Map())

    lazy val columns = SqlStr.join(
      exprs
        .map(_.asInstanceOf[Column[_]])
        .map(c => SqlStr.raw(ctx.config.columnNameMapper(c.name))),
      SqlStr.commaSep
    )

    lazy val selectSql = Renderable.toSql(select).withCompleteQuery(false)

    lazy val tableNameStr = SqlStr.raw(ctx.config.tableNameMapper(tableName))
    def render() = sql"INSERT INTO $tableNameStr ($columns) $selectSql"
  }
}
