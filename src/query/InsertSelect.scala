package usql.query

import renderer.InsertToSql
import usql.renderer.{Context, SqlStr}
import usql.Queryable
import usql.utils.OptionPickler

/**
 * Syntax reference
 *
 * https://www.postgresql.org/docs/current/sql-update.html
 */
case class InsertSelect[Q, C](insert: Insert[Q], columns: C, select: Select[C])(implicit
    val qr: Queryable[Q, _],
    qrc: Queryable[C, _]
) extends Returnable[Q] with Query{
  def expr = insert.expr
  def table = insert.table

  override def toSqlQuery(implicit ctx: Context): SqlStr =
    InsertToSql.select(
      this,
      qrc.walk(columns).map(_._2),
      ctx.tableNameMapper,
      ctx.columnNameMapper
    )

  override def isExecuteUpdate = true

  def walk() = Nil

  override def singleRow = true

}

object InsertSelect {

  implicit def InsertSelectQueryable[Q, C]: Queryable[InsertSelect[Q, C], Int] =
    new Query.Queryable[InsertSelect[Q, C], Int]()


}
