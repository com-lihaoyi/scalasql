package usql.query

import renderer.InsertToSql
import usql.renderer.{Context, SqlStr}
import usql.utils.OptionPickler
import usql.{Column, Queryable}

/**
 * Syntax reference
 *
 * https://www.postgresql.org/docs/current/sql-update.html
 */
case class InsertValues[Q, R](
    insert: Insert[Q, R],
    columns: Seq[Column.ColumnExpr[_]],
    valuesLists: Seq[Seq[Expr[_]]]
)(implicit val qr: Queryable[Q, R]) extends Returnable[Q] with Query {
  def table = insert.table
  def expr: Q = insert.expr

  override def toSqlQuery(implicit ctx: Context): SqlStr = {
    InsertToSql.values(this, ctx.tableNameMapper, ctx.columnNameMapper)
  }
  def walk() = Nil
  override def singleRow = true
  override def isExecuteUpdate = true
}

object InsertValues {

  implicit def InsertQueryable[Q, R]: Queryable[InsertValues[Q, R], Int] =
    new Query.Queryable[InsertValues[Q, R], Int]()

}
