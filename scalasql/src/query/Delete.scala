package scalasql.query

import scalasql.dialects.Dialect
import scalasql.renderer.{Context, SqlStr}
import scalasql.{Queryable, Table, TypeMapper}
import scalasql.renderer.SqlStr.SqlStringSyntax

/**
 * A SQL `DELETE` query
 */
trait Delete[Q] extends Query[Int] with Returnable[Q]

object Delete {
  class Impl[Q](val expr: Q, filter: Expr[Boolean], val table: TableRef)(implicit dialect: Dialect)
      extends Delete[Q] {
    import dialect._
    override def queryIsExecuteUpdate = true
    def queryWalkExprs() = Nil
    def queryIsSingleRow = true

    protected def renderToSql(ctx: Context) = new Renderer(table, filter, ctx).render()

    protected def queryConstruct(args: Queryable.ResultSetIterator): Int = args.get(IntType)
  }

  class Renderer(table: TableRef, expr: Expr[Boolean], prevContext: Context) {
    lazy val tableNameStr =
      SqlStr.raw(prevContext.config.tableNameMapper(Table.tableName(table.value)))
    implicit val implicitCtx = Context.compute(prevContext, Nil, Some(table))

    def render() = sql"DELETE FROM $tableNameStr WHERE $expr"
  }
}
