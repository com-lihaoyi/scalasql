package renderer

import scalasql.{Column, MappedType, Queryable}
import scalasql.query.{Expr, InsertSelect, InsertValues}
import scalasql.renderer.{Context, ExprsToSql, SelectToSql, SqlStr}
import scalasql.renderer.SqlStr.SqlStringSyntax

object InsertToSql {
  def values(q: InsertValues[_, _], prevContext: Context): SqlStr = {

    implicit val ctx = prevContext.copy(fromNaming = Map(), exprNaming = Map())
    val columns = SqlStr.join(q.columns.map(c => SqlStr.raw(ctx.columnNameMapper(c.name))), sql", ")
    val values = SqlStr.join(
      q.valuesLists
        .map(values => sql"(" + SqlStr.join(values.map(_.toSqlQuery._1), sql", ") + sql")"),
      sql", "
    )
    sql"INSERT INTO ${SqlStr.raw(ctx.tableNameMapper(q.insert.table.value.tableName))} ($columns) VALUES $values"
  }

  def select(
      q: InsertSelect[_, _, _, _],
      exprs: Seq[Expr[_]],
      prevContext: Context
  ): (SqlStr, Seq[MappedType[_]]) = {

    implicit val ctx = prevContext.copy(fromNaming = Map(), exprNaming = Map())

    val columns = SqlStr.join(
      exprs.map(_.asInstanceOf[Column.ColumnExpr[_]]).map(c =>
        SqlStr.raw(ctx.columnNameMapper(c.name))
      ),
      sql", "
    )

    val (selectSql, mappedTypes) = q.select.toSqlQuery
    (
      sql"INSERT INTO ${SqlStr.raw(ctx.tableNameMapper(q.insert.table.value.tableName))} ($columns) ${selectSql.withCompleteQuery(false)}",
      mappedTypes
    )
  }
}
