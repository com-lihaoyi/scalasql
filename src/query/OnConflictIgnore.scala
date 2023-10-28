package scalasql.query

import scalasql.renderer.SqlStr.SqlStringSyntax
import scalasql.{Column, MappedType}
import scalasql.renderer.{Context, SqlStr}

class OnConflictable[Q, R](val query: Query[R], expr: Q) {
  def onConflictIgnore(c: (Q => Column.ColumnExpr[_])*) = new OnConflictIgnore(this, c.map(_(expr)))
}

class OnConflictIgnore[Q, R](upstream: OnConflictable[Q, R],
                          columns: Seq[Column.ColumnExpr[_]]) extends Query[R] {
  def walk() = upstream.query.walk()

  def singleRow = upstream.query.singleRow

  def toSqlQuery(implicit ctx: Context): (SqlStr, Seq[MappedType[_]]) = {
    val (str, mapped) = upstream.query.toSqlQuery
    (
      str + sql" ON CONFLICT (${SqlStr.join(columns.map(c => SqlStr.raw(c.name)), sql", ")}) DO NOTHING",
      mapped
    )
  }

  override def isExecuteUpdate = true
  def valueReader = upstream.query.valueReader
}