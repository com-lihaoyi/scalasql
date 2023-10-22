package renderer

import usql.{Column, Queryable}
import usql.query.{Expr, InsertSelect, InsertValues}
import usql.renderer.{Context, ExprsToSql, SelectToSql, SqlStr}
import usql.renderer.SqlStr.SqlStringSyntax

object InsertToSql {
  def values(q: InsertValues[_, _], prevContext: Context): SqlStr = {

    implicit val ctx = prevContext.copy(fromNaming = Map(), exprNaming = Map())
    val columns = SqlStr.join(q.columns.map(c => SqlStr.raw(ctx.columnNameMapper(c.name))), usql", ")
    val values = SqlStr.join(
      q.valuesLists
        .map(values => usql"(" + SqlStr.join(values.map(_.toSqlQuery), usql", ") + usql")"),
      usql", "
    )
    usql"INSERT INTO ${SqlStr.raw(ctx.tableNameMapper(q.insert.table.value.tableName))} ($columns) VALUES $values"
  }

  def select(q: InsertSelect[_, _, _, _], exprs: Seq[Expr[_]], prevContext: Context): SqlStr = {

    implicit val ctx = prevContext.copy(fromNaming = Map(), exprNaming = Map())

    val columns = SqlStr.join(
      exprs.map(_.asInstanceOf[Column.ColumnExpr[_]]).map(c =>
        SqlStr.raw(ctx.columnNameMapper(c.name))
      ),
      usql", "
    )

    usql"INSERT INTO ${SqlStr.raw(ctx.tableNameMapper(q.insert.table.value.tableName))} ($columns) ${q.select.toSqlQuery.withCompleteQuery(false)}"
  }
}
