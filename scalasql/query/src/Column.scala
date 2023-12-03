package scalasql.query

import scalasql.core.{Context, Sql, SqlStr, TypeMapper}
import scalasql.core.SqlStr.SqlStringSyntax

/**
 * A variant of [[Sql]] representing a raw table column; allows assignment in updates
 * and inserts
 */
class Column[T](tableRef: TableRef, val name: String)(implicit val mappedType: TypeMapper[T])
    extends Sql[T] {
  def :=(v: Sql[T]) = Column.Assignment(this, v)

  def renderToSql0(implicit ctx: Context) = {
    val suffix = SqlStr.raw(ctx.config.columnNameMapper(name))
    ctx.fromNaming.get(tableRef) match {
      case Some("") => suffix
      case Some(s) => SqlStr.raw(s) + sql".$suffix"
      case None =>
        sql"SCALASQL_MISSING_TABLE_${SqlStr.raw(Table.name(tableRef.value))}.$suffix"
    }
  }
}
object Column {
  case class Assignment[T](column: Column[T], value: Sql[T])
}
