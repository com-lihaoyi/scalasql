package scalasql.query

import scalasql.renderer.{Context, SqlStr}
import scalasql.{MappedType, Queryable}
import scalasql.renderer.SqlStr.SqlStringSyntax

trait Delete[Q] extends Query[Int] with Returnable[Q]


object Delete {
  case class Impl[Q](expr: Q, filter: Expr[Boolean], table: TableRef) extends Delete[Q]{
    override def isExecuteUpdate = true
    def walk() = Nil
    def singleRow = true

    def toSqlQuery(implicit ctx: Context) = toSqlStr(table, filter, ctx)

    def valueReader = implicitly
  }
  def fromTable[Q, R](expr: Q, filter: Expr[Boolean], table: TableRef): Delete[Q] = {
    Delete.Impl(expr, filter, table)
  }

  def toSqlStr(table: TableRef, expr: Expr[Boolean], prevContext: Context) = {
    val (namedFromsMap, fromSelectables, exprNaming, context) = Context.computeContext(
      prevContext,
      Nil,
      Some(table)
    )

    implicit val ctx = context
    (
      sql"DELETE FROM ${SqlStr.raw(prevContext.tableNameMapper(table.value.tableName))} WHERE ${expr}",
      Seq(MappedType.IntType)
    )
  }
}
