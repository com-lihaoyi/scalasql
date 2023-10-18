package renderer

import usql.{Column, Queryable}
import usql.query.{Expr, InsertReturning, InsertSelect, InsertValues}
import usql.renderer.{Context, ExprsToSql, SelectToSql, SqlStr}
import usql.renderer.SqlStr.SqlStringSyntax

object InsertToSql {
  def values(q: InsertValues[_],
            tableNameMapper: String => String,
            columnNameMapper: String => String): SqlStr = {

    implicit val ctx = new Context(Map(), Map(), tableNameMapper, columnNameMapper)
    val columns = SqlStr.join(q.columns.map(c => SqlStr.raw(columnNameMapper(c.name))), usql", ")
    val values = SqlStr.join(
      q.valuesLists
        .map(values => usql"(" + SqlStr.join(values.map(_.toSqlStr), usql", ") + usql")"),
      usql", "
    )
    usql"INSERT INTO ${SqlStr.raw(tableNameMapper(q.insert.table.value.tableName))} ($columns) VALUES $values"
  }

  def select[Q, C](q: InsertSelect[Q, C],
                   exprs: Seq[Expr[_]],
                   tableNameMapper: String => String,
                   columnNameMapper: String => String): SqlStr = {

    implicit val ctx = new Context(Map(), Map(), tableNameMapper, columnNameMapper)

    val columns = SqlStr.join(
      exprs.map(_.asInstanceOf[Column.ColumnExpr[_]]).map(c => SqlStr.raw(columnNameMapper(c.name))),
      usql", "
    )

    usql"INSERT INTO ${SqlStr.raw(tableNameMapper(q.insert.table.value.tableName))} ($columns) ${q.select.toSqlStr.withCompleteQuery(false)}"
  }

  def returning[Q,R ](q: InsertReturning[Q, R],
                      qr: Queryable[Q, R],
                      tableNameMapper: String => String,
                      columnNameMapper: String => String): SqlStr = {

    implicit val (_, _, _, ctx) = SelectToSql.computeContext(
      tableNameMapper,
      columnNameMapper,
      Nil,
      Some(q.insert.insert.table),
      Map()
    )

    val prefix = values(q.insert, tableNameMapper, columnNameMapper)
    val (flattenedExpr, exprStr) = ExprsToSql.apply0(qr.walk(q.returning), ctx, usql"")
    val suffix = usql" RETURNING $exprStr"

    prefix + suffix
  }
}
