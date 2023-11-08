package scalasql.query

import scalasql.renderer.SqlStr.SqlStringSyntax
import scalasql.renderer.{Context, SqlStr}
import scalasql.{Column, MappedType, Queryable}
import scalasql.utils.OptionPickler

/**
 * A SQL `INSERT SELECT` query
 */
trait InsertSelect[Q, C, R, R2] extends InsertReturnable[Q] with Query[Int]

object InsertSelect {
  class Impl[Q, C, R, R2](insert: Insert[Q, R], columns: C, select: Select[C, R2])
      extends InsertSelect[Q, C, R, R2] {
    def expr = insert.expr

    def table = insert.table

    override def toSqlQuery(implicit ctx: Context) = (
      new Renderer(select, select.qr.walk(columns).map(_._2), ctx, table.value.tableName).render(),
      Seq(MappedType.IntType)
    )

    override def isExecuteUpdate = true

    def walk() = Nil

    override def singleRow = true

    override def valueReader: OptionPickler.Reader[Int] = implicitly
  }

  class Renderer(
      select: Select[_, _],
      exprs: Seq[Expr[_]],
      prevContext: Context,
      tableName: String
  ) {

    implicit lazy val ctx = prevContext.withExprNaming(Map()).withFromNaming(Map())

    lazy val columns = SqlStr.join(
      exprs
        .map(_.asInstanceOf[Column.ColumnExpr[_]])
        .map(c => SqlStr.raw(ctx.config.columnNameMapper(c.name))),
      sql", "
    )

    lazy val selectSql = select.toSqlQuery._1.withCompleteQuery(false)

    lazy val tableNameStr = SqlStr.raw(ctx.config.tableNameMapper(tableName))
    def render() = sql"INSERT INTO $tableNameStr ($columns) $selectSql"
  }
}
