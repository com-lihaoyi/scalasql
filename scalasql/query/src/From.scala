package scalasql.query

import scalasql.core.{Context, Queryable, Sql, SqlStr}
import scalasql.core.Context.From
import scalasql.core.SqlStr.SqlStringSyntax

/**
 * Models a SQL `FROM` clause
 */
class TableRef(val value: Table.Base) extends From {
  override def toString = s"TableRef(${Table.name(value)})"

  def fromRefPrefix(prevContext: Context) = prevContext.config.tableNameMapper(Table.name(value))
  def fromLhsMap(prevContext: Context) = Map()

  def renderSql(name: SqlStr, prevContext: Context, liveExprs: Option[Set[Sql.Identity]]) = {
    SqlStr.raw(prevContext.config.tableNameMapper(Table.name(value))) + sql" " + name
  }
}
class SubqueryRef(val value: SelectBase, val qr: Queryable[_, _]) extends From {
  def fromRefPrefix(prevContext: Context): String = "subquery"

  def fromLhsMap(prevContext: Context) = SelectBase.lhsMap(value, prevContext)

  def renderSql(name: SqlStr, prevContext: Context, liveExprs: Option[Set[Sql.Identity]]) = {
    val renderSql = SelectBase.renderer(value, prevContext)
    sql"(${renderSql.render(liveExprs)}) $name"
  }
}
class WithCteRef() extends From {
  def fromRefPrefix(prevContext: Context) = "cte"

  def fromLhsMap(prevContext: Context) = Map()

  def renderSql(name: SqlStr, prevContext: Context, liveExprs: Option[Set[Sql.Identity]]) = {
    name
  }
}
