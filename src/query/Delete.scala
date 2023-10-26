package scalasql.query

import scalasql.renderer.{Context, SqlStr}
import scalasql.{MappedType, Queryable}
import scalasql.renderer.SqlStr.SqlStringSyntax

trait Delete extends Query[Int]


object Delete {
  case class Impl(expr: Expr[Boolean], table: TableRef) extends Delete{
    override def isExecuteUpdate = true
    def walk() = Nil
    def singleRow = true

    def toSqlQuery(implicit ctx: Context) = toSqlStr(table, expr, ctx)

    def valueReader = implicitly
  }
  def fromTable[Q, R](expr: Expr[Boolean], table: TableRef)(implicit qr: Queryable[Q, R]): Delete = {
    Delete.Impl(expr, table)
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
