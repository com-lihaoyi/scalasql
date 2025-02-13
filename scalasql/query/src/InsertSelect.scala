package scalasql.query

import scalasql.core.{Context, DialectTypeMappers, Queryable, Expr, SqlStr, WithSqlExpr}
import scalasql.core.SqlStr.{Renderable, SqlStringSyntax}

/**
 * A SQL `INSERT SELECT` query
 */
trait InsertSelect[V[_[_]], C, R, R2]
    extends Returning.InsertBase[V[Column]]
    with Query.ExecuteUpdate[Int]

object InsertSelect {
  class Impl[V[_[_]], C, R, R2](insert: Insert[V, R], columns: C, select: Select[C, R2])(
      implicit dialect: DialectTypeMappers
  ) extends InsertSelect[V, C, R, R2] {
    import dialect.{dialectSelf => _, _}
    protected def expr = WithSqlExpr.get(insert)

    def table = insert.table

    private[scalasql] override def renderSql(ctx: Context) =
      new Renderer(select, select.qr.walkExprs(columns), ctx, Table.resolve(table.value)(ctx))
        .render()

    override protected def queryConstruct(args: Queryable.ResultSetIterator): Int =
      args.get(IntType)
  }

  class Renderer(
      select: Select[?, ?],
      exprs: Seq[Expr[?]],
      prevContext: Context,
      tableName: String
  ) {

    implicit lazy val ctx: Context = prevContext

    lazy val columns = SqlStr.join(
      exprs
        .map(_.asInstanceOf[Column[?]])
        .map(c => SqlStr.raw(ctx.config.columnNameMapper(c.name))),
      SqlStr.commaSep
    )

    lazy val selectSql = Renderable.renderSql(select).withCompleteQuery(false)

    lazy val tableNameStr = SqlStr.raw(tableName)
    def render() = sql"INSERT INTO $tableNameStr ($columns) $selectSql"
  }
}
