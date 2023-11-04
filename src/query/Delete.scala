package scalasql.query

import scalasql.renderer.{Context, SqlStr}
import scalasql.{MappedType, Queryable}
import scalasql.renderer.SqlStr.SqlStringSyntax

trait Delete[Q] extends Query[Int] with Returnable[Q]

object Delete {
  class Impl[Q](val expr: Q, filter: Expr[Boolean], val table: TableRef) extends Delete[Q] {
    override def isExecuteUpdate = true
    def walk() = Nil
    def singleRow = true

    def toSqlQuery(implicit ctx: Context) =
      (new Renderer(table, filter, ctx).render(), Seq(MappedType.IntType))

    def valueReader = implicitly
  }

  class Renderer(table: TableRef, expr: Expr[Boolean], prevContext: Context) {
    lazy val tableNameStr = SqlStr.raw(prevContext.config.tableNameMapper(table.value.tableName))
    lazy val computed = Context.compute(prevContext, Nil, Some(table))
    import computed.implicitCtx

    def render() = sql"DELETE FROM $tableNameStr WHERE $expr"
  }
}
