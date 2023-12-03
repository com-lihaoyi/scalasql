package scalasql.core

import scala.language.experimental.macros

/**
 * In-code representation of a SQL table, associated with a given `case class` [[V]].
 */
abstract class Table[V[_[_]]]()(implicit name: sourcecode.Name, metadata0: Table.Metadata[V])
    extends Table.Base
    with Table.LowPri[V] {

  protected[scalasql] def tableName = name.value

  protected implicit def tableSelf: Table[V] = this

  protected def tableMetadata: Table.Metadata[V] = metadata0

  implicit def containerQr(implicit dialect: DialectBase): Queryable.Row[V[Sql], V[Id]] =
    tableMetadata
      .queryable(
        tableMetadata.walkLabels0,
        dialect,
        new Table.Metadata.QueryableProxy(tableMetadata.queryables(dialect, _))
      )
      .asInstanceOf[Queryable.Row[V[Sql], V[Id]]]

  protected def tableRef = new TableRef(this)
  protected[scalasql] def tableLabels: Seq[String] = {
    tableMetadata.walkLabels0()
  }
  implicit def tableImplicitMetadata: Table.ImplicitMetadata[V] =
    Table.ImplicitMetadata(tableMetadata)
}

object Table {
  trait LowPri[V[_[_]]] { this: Table[V] =>
    implicit def containerQr2(
        implicit dialect: DialectBase
    ): Queryable.Row[V[Column], V[Id]] =
      containerQr.asInstanceOf[Queryable.Row[V[Column], V[Id]]]
  }

  case class ImplicitMetadata[V[_[_]]](value: Metadata[V])

  def tableMetadata[V[_[_]]](t: Table[V]) = t.tableMetadata
  def tableRef[V[_[_]]](t: Table[V]) = t.tableRef
  def tableName(t: Table.Base) = t.tableName
  def tableLabels(t: Table.Base) = t.tableLabels
  def tableColumnNameOverride[V[_[_]]](t: Table.Base)(s: String) = t.tableColumnNameOverride(s)
  trait Base {

    /**
     * The name of this table, before processing by [[Config.tableNameMapper]].
     * Can be overriden to configure the table names
     */
    protected[scalasql] def tableName: String
    protected[scalasql] def tableLabels: Seq[String]

    /**
     * Customizations to the column names of this table before processing,
     * by [[Config.columnNameMapper]]. Can be overriden to configure the column
     * names on a per-column basis.
     */
    protected[scalasql] def tableColumnNameOverride(s: String): String = identity(s)
  }

  class Metadata[V[_[_]]](
      val queryables: (DialectBase, Int) => Queryable.Row[_, _],
      val walkLabels0: () => Seq[String],
      val queryable: (
          () => Seq[String],
          DialectBase,
          Metadata.QueryableProxy
      ) => Queryable[V[Sql], V[Id]],
      val vExpr0: (TableRef, DialectBase, Metadata.QueryableProxy) => V[Column]
  ) {
    def vExpr(t: TableRef, d: DialectBase) =
      vExpr0(t, d, new Metadata.QueryableProxy(queryables(d, _)))
  }

  object Metadata extends scalasql.core.TableMacros {
    class QueryableProxy(queryables: Int => Queryable.Row[_, _]) {
      def apply[T, V](n: Int): Queryable.Row[T, V] = queryables(n).asInstanceOf[Queryable.Row[T, V]]
    }
  }

  object Internal {
    class TableQueryable[Q, R <: scala.Product](
        walkLabels0: () => Seq[String],
        walkExprs0: Q => Seq[Sql[_]],
        construct0: Queryable.ResultSetIterator => R,
        deconstruct0: R => Q = ???
    ) extends Queryable.Row[Q, R] {
      def walkLabels(): Seq[List[String]] = walkLabels0().map(List(_))
      def walkExprs(q: Q): Seq[Sql[_]] = walkExprs0(q)

      def construct(args: Queryable.ResultSetIterator) = construct0(args)

      def deconstruct(r: R): Q = deconstruct0(r)
    }

  }
}
