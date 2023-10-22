package usql.query

import renderer.InsertToSql
import usql.renderer.{Context, SqlStr}
import usql.Queryable

/**
 * Syntax reference
 *
 * https://www.postgresql.org/docs/current/sql-update.html
 */
case class InsertSelect[Q, C, R, R2](insert: Insert[Q, R],
                                     columns: C,
                                     select: Select[C, R2]) extends Returnable[Q] with Query {
  def expr = insert.expr
  def table = insert.table

  override def toSqlQuery(implicit ctx: Context): SqlStr =
    InsertToSql.select(
      this,
      select.qr.walk(columns).map(_._2),
      ctx.tableNameMapper,
      ctx.columnNameMapper
    )

  override def isExecuteUpdate = true

  def walk() = Nil

  override def singleRow = true

}

object InsertSelect {

  implicit def InsertSelectQueryable[Q, C, R, R2]: Queryable[InsertSelect[Q, C, R, R2], Int] =
    new Query.Queryable[InsertSelect[Q, C, R, R2], Int]()

}
