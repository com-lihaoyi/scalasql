package scalasql.core

/**
 * Models a SQL `FROM` clause
 */
sealed trait From
class TableRef(val value: Table.Base) extends From {
  override def toString = s"TableRef(${Table.tableName(value)})"
}
class SubqueryRef(val value: SelectBase, val qr: Queryable[_, _]) extends From
class WithCteRef() extends From
