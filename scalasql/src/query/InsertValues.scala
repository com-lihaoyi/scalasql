package scalasql.query

import scalasql.{Queryable, ResultSetIterator, Table}
import scalasql.dialects.Dialect
import scalasql.renderer.SqlStr.{Renderable, SqlStringSyntax}
import scalasql.renderer.{Context, SqlStr}

trait InsertValues[Q, R] extends Query[Int]
object InsertValues {
  class Impl[Q, R](insert: Insert[Q, R], values: Seq[R], dialect: Dialect, qr: Queryable.Row[Q, R])
      extends InsertValues[Q, R] {
    override protected def queryWalkExprs(): Seq[(List[String], Expr[_])] = Nil

    override protected def queryIsSingleRow: Boolean = true

    protected override def queryIsExecuteUpdate = true

    override protected def queryConstruct(args: ResultSetIterator): Int = args.get(dialect.IntType)

    override protected def renderToSql(ctx: Context): SqlStr = {
      new Renderer(Table.tableName(insert.table.value), values, ctx, qr).render()
    }
  }
  class Renderer[Q, R](
      tableName: String,
      valuesList: Seq[R],
      ctx: Context,
      qr: Queryable.Row[Q, R]
  ) {
    lazy val values = SqlStr.join(
      valuesList.map(v =>
        sql"(" + SqlStr.join(qr.deconstruct(v).map(i => sql"$i"), sql", ") + sql")"
      ),
      sql", "
    )
    def render() = {
      sql"INSERT INTO ${SqlStr.raw(ctx.config.tableNameMapper(tableName))} VALUES $values"
    }
  }
}
