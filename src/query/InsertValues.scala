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
case class InsertValues[Q](
    insert: Insert[Q],
    columns: Seq[Column.ColumnExpr[_]],
    valuesLists: Seq[Seq[Expr[_]]]
)(implicit val qr: Queryable[Q, _]) extends Returnable[Q] {
  def table = insert.table
  def expr: Q = insert.expr

  override def toSqlQuery(implicit ctx: Context): SqlStr = {
    InsertToSql.values(this, ctx.tableNameMapper, ctx.columnNameMapper)
  }
}

object InsertValues {

  implicit def InsertQueryable[Q](implicit qr: Queryable[Q, _]): Queryable[InsertValues[Q], Int] =
    new InsertQueryable[Q]()(qr)

  class InsertQueryable[Q](implicit qr: Queryable[Q, _]) extends Queryable[InsertValues[Q], Int] {
    override def isExecuteUpdate = true
    def walk(ur: InsertValues[Q]): Seq[(List[String], Expr[_])] = Nil

    override def singleRow = true

    def valueReader: OptionPickler.Reader[Int] = OptionPickler.IntReader

    override def toSqlQuery(q: InsertValues[Q], ctx0: Context): SqlStr = q.toSqlQuery(ctx0)
  }
}
