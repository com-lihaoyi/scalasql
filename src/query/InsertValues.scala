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
)(implicit val qr: Queryable[Q, R]) extends Returnable[Q] with Query[Int] {
  def table = insert.table
  def expr: Q = insert.expr

  override def toSqlQuery(implicit ctx: Context): SqlStr = InsertToSql.values(this, ctx)
  def walk() = Nil
  override def singleRow = true
  override def isExecuteUpdate = true

  override def valueReader: OptionPickler.Reader[Int] = implicitly
}

