package scalasql.query

import scalasql.renderer.SqlStr.SqlStringSyntax
import scalasql.renderer.{Context, SqlStr}
import scalasql.{Column, MappedType, Queryable}
import scalasql.utils.OptionPickler

/**
 * Syntax reference
 *
 * https://www.postgresql.org/docs/current/sql-update.html
 */
trait InsertSelect[Q, C, R, R2]
    extends Returnable[Q] with Query[Int]

object InsertSelect{
  case class Impl[Q, C, R, R2](insert: Insert[Q, R], columns: C, select: Select[C, R2])
    extends InsertSelect[Q, C, R, R2] {
    def expr = insert.expr

    def table = insert.table

    override def toSqlQuery(implicit ctx: Context) =
      toSqlStr(select, select.qr.walk(columns).map(_._2), ctx, table.value.tableName)

    override def isExecuteUpdate = true

    def walk() = Nil

    override def singleRow = true

    override def valueReader: OptionPickler.Reader[Int] = implicitly
  }


  def toSqlStr(
              select: Select[_, _],
              exprs: Seq[Expr[_]],
              prevContext: Context,
              tableName: String
            ): (SqlStr, Seq[MappedType[_]]) = {

    implicit val ctx = prevContext.copy(fromNaming = Map(), exprNaming = Map())

    val columns = SqlStr.join(
      exprs.map(_.asInstanceOf[Column.ColumnExpr[_]]).map(c =>
        SqlStr.raw(ctx.columnNameMapper(c.name))
      ),
      sql", "
    )

    val (selectSql, mappedTypes) = select.toSqlQuery
    (
      sql"INSERT INTO ${SqlStr.raw(ctx.tableNameMapper(tableName))} ($columns) ${selectSql.withCompleteQuery(false)}",
      mappedTypes
    )
  }
}