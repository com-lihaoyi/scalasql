package scalasql
import scala.language.experimental.macros
import renderer.{Context, ExprsToSql, JoinsToSql, SqlStr}
import scalasql.query.{Expr, Insert, InsertColumns, Joinable, Select, TableRef, Update}
import renderer.SqlStr.SqlStringSyntax
import scalasql.dialects.Dialect

/**
 * In-code representation of a SQL table, associated with a given `case class` [[V]].
 */
abstract class Table[V[_[_]]]()(implicit name: sourcecode.Name, metadata0: Table.Metadata[V])
    extends Table.Base
    with Table.LowPri[V] {

  /**
   * The name of this table, before processing by [[Config.tableNameMapper]].
   * Can be overriden to configure the table names
   */
  protected[scalasql] def tableName = name.value

  /**
   * Customizations to the column names of this table before processing,
   * by [[Config.columnNameMapper]]. Can be overriden to configure the column
   * names on a per-column basis.
   */
  protected def tableColumnNameOverride(s: String): String = identity(s)
  protected implicit def tableSelf: Table[V] = this

  protected def tableMetadata: Table.Metadata[V] = metadata0

  implicit def containerQr(implicit dialect: Dialect): Queryable.Row[V[Expr], V[Id]] =
    tableMetadata
      .queryable(
        tableMetadata.walkLabels0,
        dialect,
        new Table.Metadata.QueryableProxy(tableMetadata.queryables(dialect, _))
      )
      .asInstanceOf[Queryable.Row[V[Expr], V[Id]]]

  protected def tableRef = new scalasql.query.TableRef(this)
  protected[scalasql] def tableLabels: Seq[String] = {
    tableMetadata.walkLabels0()
  }
  implicit def tableImplicitMetadata: Table.ImplicitMetadata[V] =
    Table.ImplicitMetadata(tableMetadata)
}

object Table {
  trait LowPri[V[_[_]]] { this: Table[V] =>
    implicit def containerQr2(
        implicit dialect: Dialect
    ): Queryable.Row[V[Column.ColumnExpr], V[Id]] =
      containerQr.asInstanceOf[Queryable.Row[V[Column.ColumnExpr], V[Id]]]
  }

  case class ImplicitMetadata[V[_[_]]](value: Metadata[V])

  def tableMetadata[V[_[_]]](t: Table[V]) = t.tableMetadata
  def tableRef[V[_[_]]](t: Table[V]) = t.tableRef
  def tableName(t: Table.Base) = t.tableName
  def tableLabels(t: Table.Base) = t.tableLabels
  def tableColumnNameOverride[V[_[_]]](t: Table[V])(s: String) = t.tableColumnNameOverride(s)
  trait Base {
    protected[scalasql] def tableName: String
    protected[scalasql] def tableLabels: Seq[String]
  }

  class Metadata[V[_[_]]](
      val queryables: (Dialect, Int) => Queryable.Row[_, _],
      val walkLabels0: () => Seq[String],
      val queryable: (
          () => Seq[String],
          Dialect,
          Metadata.QueryableProxy
      ) => Queryable[V[Expr], V[Id]],
      val vExpr0: (TableRef, Dialect, Metadata.QueryableProxy) => V[Column.ColumnExpr]
  ) {
    def vExpr(t: TableRef, d: Dialect) = vExpr0(t, d, new Metadata.QueryableProxy(queryables(d, _)))
  }

  object Metadata extends scalasql.utils.TableMacros {
    class QueryableProxy(queryables: Int => Queryable.Row[_, _]) {
      def apply[T, V](n: Int): Queryable.Row[T, V] = queryables(n).asInstanceOf[Queryable.Row[T, V]]
    }
  }

  object Internal {
    class TableQueryable[Q, R <: scala.Product](
        walkLabels0: () => Seq[String],
        walkExprs0: Q => Seq[Expr[_]],
        construct0: Queryable.ResultSetIterator => R,
        deconstruct0: R => Q = ???
    ) extends Queryable.Row[Q, R] {
      def walkLabels(): Seq[List[String]] = walkLabels0().map(List(_))
      def walkExprs(q: Q): Seq[Expr[_]] = walkExprs0(q)

      def construct(args: Queryable.ResultSetIterator) = construct0(args)

      def deconstruct(r: R): Q = deconstruct0(r)
    }
  }
}

case class Column[T: TypeMapper]()(implicit val name: sourcecode.Name, val table: Table.Base) {
  def expr(tableRef: TableRef): Column.ColumnExpr[T] =
    new Column.ColumnExpr[T](tableRef, name.value)
}

object Column {
  case class Assignment[T](column: ColumnExpr[T], value: Expr[T])
  class ColumnExpr[T](tableRef: TableRef, val name: String)(implicit val mappedType: TypeMapper[T])
      extends Expr[T] {
    def :=(v: Expr[T]) = Assignment(this, v)
    def renderToSql0(implicit ctx: Context) = {
      val suffix = SqlStr.raw(ctx.config.columnNameMapper(name))
      ctx.fromNaming.get(tableRef) match {
        case Some("") => suffix
        case Some(s) => SqlStr.raw(s) + sql".$suffix"
        case None =>
          sql"SCALASQL_MISSING_TABLE_${SqlStr.raw(Table.tableName(tableRef.value))}.$suffix"
      }
    }
  }
}
