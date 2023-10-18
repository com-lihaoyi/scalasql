package usql.query

import renderer.InsertToSql
import usql.renderer.{Context, SqlStr}
import usql.{Column, OptionPickler, Queryable}


/**
 * Syntax reference
 *
 * https://www.postgresql.org/docs/current/sql-update.html
 */
case class InsertValues[Q](insert: Insert[Q],
                           columns: Seq[Column.ColumnExpr[_]],
                           valuesLists: Seq[Seq[Expr[_]]])
                          (implicit val qr: Queryable[Q, _]) extends Returnable[Q] {
  def table = insert.table
  def expr: Q = insert.expr

  override def toSqlQuery(implicit ctx: Context): SqlStr =
    InsertValues.InsertQueryable(qr).toSqlQuery(this, ctx)
}

object InsertValues {


  implicit def InsertQueryable[Q](implicit qr: Queryable[Q, _]): Queryable[InsertValues[Q], Int] =
    new InsertQueryable[Q]()(qr)

  class InsertQueryable[Q](implicit qr: Queryable[Q, _]) extends Queryable[InsertValues[Q], Int] {
    override def isExecuteUpdate = true
    def walk(ur: InsertValues[Q]): Seq[(List[String], Expr[_])] = Nil

    override def singleRow = true

    def valueReader: OptionPickler.Reader[Int] = OptionPickler.IntReader

    override def toSqlQuery(q: InsertValues[Q], ctx0: Context): SqlStr = {
      InsertToSql.values(q, ctx0.tableNameMapper, ctx0.columnNameMapper)
    }
  }
}
