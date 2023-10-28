package scalasql.query

import scalasql.renderer.SqlStr.SqlStringSyntax
import scalasql.{Column, MappedType}
import scalasql.renderer.{Context, SqlStr}

class OnConflict[Q, R](val query: Query[R], expr: Q) {
  def onConflictIgnore(c: (Q => Column.ColumnExpr[_])*) = new OnConflict.Ignore(this, c.map(_(expr)))
  def onConflictUpdate(c: (Q => Column.ColumnExpr[_])*)(c2: (Q => (Column.ColumnExpr[_], Expr[_]))*) =
    new OnConflict.Update(this, c.map(_(expr)), c2.map(_(expr)))
}

object OnConflict {
  class Ignore[Q, R](upstream: OnConflict[Q, R],
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

  class Update[Q, R](upstream: OnConflict[Q, R],
                     columns: Seq[Column.ColumnExpr[_]],
                     updates: Seq[(Column.ColumnExpr[_], Expr[_])]) extends Query[R] {
    def walk() = upstream.query.walk()

    def singleRow = upstream.query.singleRow

    def toSqlQuery(implicit ctx: Context): (SqlStr, Seq[MappedType[_]]) = {
      val (str, mapped) = upstream.query.toSqlQuery
      val columnsStr = SqlStr.join(columns.map(c => SqlStr.raw(c.name)), sql", ")
      val updatesStr = SqlStr.join(updates.map{case (c, e) => SqlStr.raw(c.name) + sql" = $e"}, sql", ")
      (
        str + sql" ON CONFLICT (${columnsStr}) DO UPDATE SET $updatesStr",
        mapped
      )
    }

    override def isExecuteUpdate = true

    def valueReader = upstream.query.valueReader
  }
}