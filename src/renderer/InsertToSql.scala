package renderer

import usql.Queryable
import usql.query.{Insert, InsertReturning}
import usql.renderer.{Context, ExprsToSql, SelectToSql, SqlStr}
import usql.renderer.SqlStr.SqlStringSyntax

object InsertToSql {
  def apply(q: Insert[_],
            tableNameMapper: String => String,
            columnNameMapper: String => String): SqlStr = {

    implicit val ctx = new Context(Map(), Map(), tableNameMapper, columnNameMapper)
    val columns = SqlStr.join(q.columns.map(c => SqlStr.raw(columnNameMapper(c.name))), usql", ")
    val values = SqlStr.join(
      q.valuesLists
        .map(values => usql"(" + SqlStr.join(values.map(_.toSqlStr), usql", ") + usql")"),
      usql", "
    )
    usql"INSERT INTO ${SqlStr.raw(tableNameMapper(q.table.value.tableName))} ($columns) VALUES $values"
  }

  def returning[Q,R ](q: InsertReturning[Q, R],
                      qr: Queryable[Q, R],
                      tableNameMapper: String => String,
                      columnNameMapper: String => String): SqlStr = {

    implicit val (_, _, _, ctx) = SelectToSql.computeContext(
      tableNameMapper,
      columnNameMapper,
      Nil,
      Some(q.insert.table),
      Map()
    )

    val prefix = apply(q.insert, tableNameMapper, columnNameMapper)
    val (flattenedExpr, exprStr) = ExprsToSql.apply0(qr.walk(q.returning), ctx, usql"")
    val suffix = usql" RETURNING $exprStr"

    prefix + suffix
  }
}
