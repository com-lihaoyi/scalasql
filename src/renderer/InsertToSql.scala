package renderer

import usql.Queryable
import usql.query.{Insert, Update, UpdateReturning}
import usql.renderer.{Context, SelectToSql, SqlStr}
import usql.renderer.SqlStr.{SqlStringSyntax, optSeq}

object InsertToSql {
  def apply[Q](q: Insert[Q],
               qr: Queryable[Q, _],
               tableNameMapper: String => String,
               columnNameMapper: String => String) = {

    implicit val ctx = new Context(Map(), Map(), identity, identity)
    val columns = SqlStr.join(q.columns.map(c => SqlStr.raw(columnNameMapper(c.name))), usql", ")
    val values = SqlStr.join(
      q.valuesLists
        .map(values => usql"(" + SqlStr.join(values.map(_.toSqlStr), usql", ") + usql")"),
      usql", "
    )
    usql"INSERT INTO ${SqlStr.raw(tableNameMapper(q.table.value.tableName))} ($columns) VALUES $values"
  }
}
