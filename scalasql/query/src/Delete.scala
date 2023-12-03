package scalasql.query

import scalasql.core.DialectBase
import scalasql.core.Context
import scalasql.core.{Queryable, SqlStr, Sql}
import scalasql.core.SqlStr.SqlStringSyntax

/**
 * A SQL `DELETE` query
 */
trait Delete[Q] extends Query[Int] with Returnable[Q]

object Delete {
  class Impl[Q](val expr: Q, filter: Sql[Boolean], val table: TableRef)(
      implicit dialect: DialectBase
  ) extends Delete[Q] {
    import dialect._
    override def queryIsExecuteUpdate = true
    protected def queryWalkLabels() = Nil
    protected def queryWalkExprs() = Nil
    protected def queryIsSingleRow = true

    protected def renderToSql(ctx: Context) = new Renderer(table, filter, ctx).render()

    protected def queryConstruct(args: Queryable.ResultSetIterator): Int = args.get(IntType)
  }

  class Renderer(table: TableRef, expr: Sql[Boolean], prevContext: Context) {
    lazy val tableNameStr =
      SqlStr.raw(prevContext.config.tableNameMapper(Table.name(table.value)))
    implicit val implicitCtx = Context.compute(prevContext, Nil, Some(table))

    def render() = sql"DELETE FROM $tableNameStr WHERE $expr"
  }
}
