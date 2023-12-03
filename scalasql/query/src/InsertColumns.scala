package scalasql.query

import scalasql.core.{Context, DialectBase, Queryable, Sql, SqlStr, WithExpr}
import scalasql.core.SqlStr.{Renderable, SqlStringSyntax}

/**
 * A SQL `INSERT VALUES` query
 */
trait InsertColumns[V[_[_]], R] extends InsertReturnable[V[Column]] with Query[Int] {
  def columns: Seq[Column[_]]
  def valuesLists: Seq[Seq[Sql[_]]]
}
object InsertColumns {
  class Impl[V[_[_]], R](
      insert: Insert[V, R],
      val columns: Seq[Column[_]],
      val valuesLists: Seq[Seq[Sql[_]]]
  )(implicit val qr: Queryable[V[Column], R], dialect: DialectBase)
      extends InsertColumns[V, R] {
    import dialect.{dialectSelf => _, _}
    def table = insert.table
    protected def expr: V[Column] = WithExpr.get(insert)

    protected override def renderToSql(ctx: Context) =
      new Renderer(columns, ctx, valuesLists, Table.name(table.value)).render()

    protected def queryWalkLabels() = Nil
    protected def queryWalkExprs() = Nil
    protected override def queryIsSingleRow = true
    protected override def queryIsExecuteUpdate = true

    override protected def queryConstruct(args: Queryable.ResultSetIterator): Int =
      args.get(IntType)
  }

  class Renderer(
      columns0: Seq[Column[_]],
      prevContext: Context,
      valuesLists: Seq[Seq[Sql[_]]],
      tableName: String
  ) {

    implicit lazy val ctx: Context = prevContext.withExprNaming(Map()).withFromNaming(Map())
    lazy val columns = SqlStr
      .join(columns0.map(c => SqlStr.raw(ctx.config.columnNameMapper(c.name))), SqlStr.commaSep)
    lazy val values = SqlStr.join(
      valuesLists
        .map(values =>
          sql"(" + SqlStr.join(values.map(Renderable.toSql(_)), SqlStr.commaSep) + sql")"
        ),
      SqlStr.commaSep
    )
    def render() = {
      sql"INSERT INTO ${SqlStr.raw(ctx.config.tableNameMapper(tableName))} ($columns) VALUES $values"
    }
  }
}
