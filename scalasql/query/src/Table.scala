package scalasql.query

import scalasql.core.{DialectTypeMappers, Sc, Queryable, Expr}
import scalasql.core.Context

abstract class Table0[VExpr, VCol, VRow]()(implicit name: sourcecode.Name, metadata0: Table0.Metadata[VExpr, VCol, VRow])
    extends Table.Base
    with Table0.LowPri[VExpr, VCol, VRow] {
  protected[scalasql] def tableName = name.value

  protected[scalasql] def schemaName = ""

  protected[scalasql] def escape: Boolean = false

  protected implicit def tableSelf: Table0[VExpr, VCol, VRow] = this

  protected def tableMetadata: Table0.Metadata[VExpr, VCol, VRow] = metadata0

  implicit def containerQr(implicit dialect: DialectTypeMappers): Queryable.Row[VExpr, VRow] =
    tableMetadata
      .queryable(
        tableMetadata.walkLabels0,
        dialect,
        new Table.Metadata.QueryableProxy(tableMetadata.queryables(dialect, _))
      )
      .asInstanceOf[Queryable.Row[VExpr, VRow]]

  protected def tableRef = new TableRef(this)
  protected[scalasql] def tableLabels: Seq[String] = {
    tableMetadata.walkLabels0()
  }
}

object Table0 {
  trait LowPri[VExpr, VCol, VRow] { this: Table0[VExpr, VCol, VRow] =>
    implicit def containerQr2(
        implicit dialect: DialectTypeMappers
    ): Queryable.Row[VCol, VRow] =
      containerQr.asInstanceOf[Queryable.Row[VCol, VRow]]
  }

  def metadata[VExpr, VCol, VRow](t: Table0[VExpr, VCol, VRow]) = t.tableMetadata
  def ref[VExpr, VCol, VRow](t: Table0[VExpr, VCol, VRow]) = t.tableRef
  def name(t: Table.Base) = t.tableName
  def labels(t: Table.Base) = t.tableLabels
  def columnNameOverride(t: Table.Base)(s: String) = t.tableColumnNameOverride(s)
  def identifier(t: Table.Base)(implicit context: Context): String = {
    context.config.tableNameMapper.andThen { str =>
      if (t.escape) {
        context.dialectConfig.escape(str)
      } else {
        str
      }
    }(t.tableName)
  }
  def fullIdentifier(
      t: Table.Base
  )(implicit context: Context): String = {
    t.schemaName match {
      case "" => identifier(t)
      case str => s"$str." + identifier(t)
    }
  }

  class Metadata[VExpr, VCol, VRow](
      val queryables: (DialectTypeMappers, Int) => Queryable.Row[?, ?],
      val walkLabels0: () => Seq[String],
      val queryable: (
          () => Seq[String],
          DialectTypeMappers,
          Metadata.QueryableProxy
      ) => Queryable[VExpr, VRow],
      val vExpr0: (TableRef, DialectTypeMappers, Metadata.QueryableProxy) => VCol
  ) {
    def vExpr(t: TableRef, d: DialectTypeMappers): VCol =
      vExpr0(t, d, new Metadata.QueryableProxy(queryables(d, _)))
  }

  object Metadata {
    class QueryableProxy(queryables: Int => Queryable.Row[?, ?]) {
      def apply[T, V](n: Int): Queryable.Row[T, V] = queryables(n).asInstanceOf[Queryable.Row[T, V]]
    }
  }
}

/**
 * In-code representation of a SQL table, associated with a given `case class` [[V]].
 */
abstract class Table[V[_[_]]]()(implicit name: sourcecode.Name, metadata0: Table.Metadata[V])
    extends Table0[V[Expr], V[Column], V[Sc]]()(name, metadata0){

  implicit def tableImplicitMetadata: Table.ImplicitMetadata[V] =
    Table.ImplicitMetadata(metadata0)
}

object Table {

  class Metadata[V[_[_]]](
    queryables: (DialectTypeMappers, Int) => Queryable.Row[?, ?],
    walkLabels0: () => Seq[String],
    queryable: (
        () => Seq[String],
        DialectTypeMappers,
        Metadata.QueryableProxy
    ) => Queryable[V[Expr], V[Sc]],
    vExpr0: (TableRef, DialectTypeMappers, Metadata.QueryableProxy) => V[Column]
  ) extends Table0.Metadata[V[Expr], V[Column], V[Sc]](
    queryables,
    walkLabels0,
    queryable,
    vExpr0
  )

  object Metadata extends scalasql.query.TableMacros {
    type QueryableProxy = Table0.Metadata.QueryableProxy
  }

  case class ImplicitMetadata[V[_[_]]](value: Metadata[V])

  def metadata[V[_[_]]](t: Table[V]) = Table0.metadata(t)
  def ref[V[_[_]]](t: Table[V]) = Table0.ref(t)
  def name(t: Table.Base) = Table0.name(t)
  def labels(t: Table.Base) = Table0.labels(t)
  def columnNameOverride[V[_[_]]](t: Table.Base)(s: String) = Table0.columnNameOverride(t)(s)
  def identifier(t: Table.Base)(implicit context: Context): String = Table0.identifier(t)(context)
  def fullIdentifier(
      t: Table.Base
  )(implicit context: Context): String = Table0.fullIdentifier(t)(context)

  trait Base {

    /**
     * The name of this table, before processing by [[Config.tableNameMapper]].
     * Can be overriden to configure the table names
     */
    protected[scalasql] def tableName: String
    protected[scalasql] def schemaName: String
    protected[scalasql] def tableLabels: Seq[String]
    protected[scalasql] def escape: Boolean

    /**
     * Customizations to the column names of this table before processing,
     * by [[Config.columnNameMapper]]. Can be overriden to configure the column
     * names on a per-column basis.
     */
    protected[scalasql] def tableColumnNameOverride(s: String): String = identity(s)
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
