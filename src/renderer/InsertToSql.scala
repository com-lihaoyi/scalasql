package renderer

import usql.{Column, Queryable}
import usql.query.{Expr, InsertSelect, InsertValues}
import usql.renderer.{Context, ExprsToSql, SelectToSql, SqlStr}
import usql.renderer.SqlStr.SqlStringSyntax

object InsertToSql {
  def values(
      q: InsertValues[_, _],
      tableNameMapper: String => String,
      columnNameMapper: String => String
  ): SqlStr = {

    implicit val ctx = new Context(Map(), Map(), tableNameMapper, columnNameMapper)
    val columns = SqlStr.join(q.columns.map(c => SqlStr.raw(columnNameMapper(c.name))), usql", ")
    val values = SqlStr.join(
      q.valuesLists
        .map(values => usql"(" + SqlStr.join(values.map(_.toSqlQuery), usql", ") + usql")"),
      usql", "
    )
    usql"INSERT INTO ${SqlStr.raw(tableNameMapper(q.insert.table.value.tableName))} ($columns) VALUES $values"
  }

  def select(
      q: InsertSelect[_, _, _, _],
      exprs: Seq[Expr[_]],
      tableNameMapper: String => String,
      columnNameMapper: String => String
  ): SqlStr = {

    implicit val ctx = new Context(Map(), Map(), tableNameMapper, columnNameMapper)

    val columns = SqlStr.join(
      exprs.map(_.asInstanceOf[Column.ColumnExpr[_]]).map(c =>
        SqlStr.raw(columnNameMapper(c.name))
      ),
      usql", "
    )

    usql"INSERT INTO ${SqlStr.raw(tableNameMapper(q.insert.table.value.tableName))} ($columns) ${q.select.toSqlQuery.withCompleteQuery(false)}"
  }
}
