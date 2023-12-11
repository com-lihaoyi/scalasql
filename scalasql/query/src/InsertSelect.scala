package scalasql.query

import scalasql.core.{Context, DialectTypeMappers, Queryable, Expr, SqlStr, WithSqlExpr}
import scalasql.core.SqlStr.{Renderable, SqlStringSyntax}

/**
 * A SQL `INSERT SELECT` query
 */
trait InsertSelect[V[_[_]], C, R, R2]
    extends Returning.InsertBase[V[Expr]]
    with Query.ExecuteUpdate[Int]

object InsertSelect {
  class Impl[V[_[_]], C, R, R2](insert: Insert[V, R], columns: C, select: Select[C, R2])(
      implicit dialect: DialectTypeMappers
  ) extends InsertSelect[V, C, R, R2] {
    import dialect.{dialectSelf => _, _}
    protected def expr = WithSqlExpr.get(insert).asInstanceOf[V[Expr]]

    def table = insert.table

    protected override def renderSql(ctx: Context) =
      new Renderer(select, select.qr.walkExprs(columns), ctx, Table.name(table.value))
        .render()

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
        .map(_.asInstanceOf[Column[_]])
        .map(c => SqlStr.raw(ctx.config.columnNameMapper(c.name))),
      SqlStr.commaSep
    )

    lazy val selectSql = Renderable.renderSql(select).withCompleteQuery(false)

    lazy val tableNameStr = SqlStr.raw(ctx.config.tableNameMapper(tableName))
    def render() = sql"INSERT INTO $tableNameStr ($columns) $selectSql"
  }
}
