package scalasql.query

import scalasql.core.{DialectTypeMappers, Sc, Queryable, Expr}
import scalasql.core.Context

/**
 * In-code representation of a SQL table, associated with a given `case class` [[V]].
 */
abstract class Table[V[_[_]]]()(implicit name: sourcecode.Name, metadata0: Table.Metadata[V])
    extends Table.Base
    with Table.LowPri[V] {

  protected[scalasql] def tableName = name.value

  protected[scalasql] def schemaName = ""

  protected implicit def tableSelf: Table[V] = this

  protected def tableMetadata: Table.Metadata[V] = metadata0

  implicit def containerQr(implicit dialect: DialectTypeMappers): Queryable.Row[V[Expr], V[Sc]] =
    tableMetadata
      .queryable(
        tableMetadata.walkLabels0,
        dialect,
        new Table.Metadata.QueryableProxy(tableMetadata.queryables(dialect, _))
      )
      .asInstanceOf[Queryable.Row[V[Expr], V[Sc]]]

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
        implicit dialect: DialectTypeMappers
    ): Queryable.Row[V[Column], V[Sc]] =
      containerQr.asInstanceOf[Queryable.Row[V[Column], V[Sc]]]
  }

  case class ImplicitMetadata[V[_[_]]](value: Metadata[V])

  def metadata[V[_[_]]](t: Table[V]) = t.tableMetadata
  def ref[V[_[_]]](t: Table[V]) = t.tableRef
  def name(t: Table.Base) = t.tableName
  def labels(t: Table.Base) = t.tableLabels
  def columnNameOverride[V[_[_]]](t: Table.Base)(s: String) = t.tableColumnNameOverride(s)
  def resolve(t: Table.Base)(implicit context: Context) = {
    val mappedTableName = context.config.tableNameMapper(t.tableName)
    t.schemaName match {
      case "" => mappedTableName
      case str => s"$str." + mappedTableName
    }
  }
  trait Base {

    /**
     * The name of this table, before processing by [[Config.tableNameMapper]].
     * Can be overriden to configure the table names
     */
    protected[scalasql] def tableName: String
    protected[scalasql] def schemaName: String
    protected[scalasql] def tableLabels: Seq[String]

    /**
     * Customizations to the column names of this table before processing,
     * by [[Config.columnNameMapper]]. Can be overriden to configure the column
     * names on a per-column basis.
     */
    protected[scalasql] def tableColumnNameOverride(s: String): String = identity(s)
  }

  class Metadata[V[_[_]]](
      val queryables: (DialectTypeMappers, Int) => Queryable.Row[?, ?],
      val walkLabels0: () => Seq[String],
      val queryable: (
          () => Seq[String],
          DialectTypeMappers,
          Metadata.QueryableProxy
      ) => Queryable[V[Expr], V[Sc]],
      val vExpr0: (TableRef, DialectTypeMappers, Metadata.QueryableProxy) => V[Column]
  ) {
    def vExpr(t: TableRef, d: DialectTypeMappers) =
      vExpr0(t, d, new Metadata.QueryableProxy(queryables(d, _)))
  }

  object Metadata extends scalasql.query.TableMacros {
    class QueryableProxy(queryables: Int => Queryable.Row[?, ?]) {
      def apply[T, V](n: Int): Queryable.Row[T, V] = queryables(n).asInstanceOf[Queryable.Row[T, V]]
    }
  }

  object Internal {
    class TableQueryable[Q, R <: scala.Product](
        walkLabels0: () => Seq[String],
        walkExprs0: Q => Seq[Expr[?]],
        construct0: Queryable.ResultSetIterator => R,
        deconstruct0: R => Q = ???
    ) extends Queryable.Row[Q, R] {
      def walkLabels(): Seq[List[String]] = walkLabels0().map(List(_))
      def walkExprs(q: Q): Seq[Expr[?]] = walkExprs0(q)

      def construct(args: Queryable.ResultSetIterator) = construct0(args)

      def deconstruct(r: R): Q = deconstruct0(r)
    }

  }
}
