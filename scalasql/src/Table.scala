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
    extends Table.Base {

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

  implicit def containerQr[E[_] <: Expr[_]](implicit dialect: Dialect): Queryable.Row[V[E], V[Id]] =
    tableMetadata
      .queryable(dialect)
      .asInstanceOf[Queryable.Row[V[E], V[Id]]]

  protected def tableRef = new scalasql.query.TableRef(this)
  protected[scalasql] def tableLabels: Seq[String] = {
    tableMetadata.walkLabels0().map(_.head)
  }
  implicit def tableImplicitMetadata: Table.ImplicitMetadata[V] = Table.ImplicitMetadata(tableMetadata)
}

object Table {
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
      val walkLabels0: () => Seq[List[String]],
      val queryable: Dialect => Queryable[V[Expr], V[Id]],
      val vExpr: (TableRef, Dialect) => V[Column.ColumnExpr]
  )
  object Metadata extends scalasql.utils.TableMacros
  object Internal {
    class TableQueryable[Q, R <: scala.Product](
        walkLabels0: () => Seq[List[String]],
        walkExprs0: Q => Seq[Expr[_]],
        construct0: ResultSetIterator => R,
        deconstruct0: Seq[Any => Seq[SqlStr.Interp.TypeInterp[_]]]
    ) extends Queryable.Row[Q, R] {
      def walkLabels(): Seq[List[String]] = walkLabels0()
      def walkExprs(q: Q): Seq[Expr[_]] = walkExprs0(q)

      def construct(args: ResultSetIterator) = construct0(args)
      def deconstruct(r: R) = {
        r.asInstanceOf[scala.Product].productIterator.zip(deconstruct0).flatMap { case (v, d) => (d(v)) }.toSeq
      }

      def toSqlStr(q: Q, ctx: Context): SqlStr = {
        ExprsToSql(this.walk(q), SqlStr.empty, ctx)
      }

      def deconstruct2(r: R): Q = ???
    }

    def flattenPrefixedLists[T](
        prefix: String
    )(implicit q: Queryable.Row[T, _]): Seq[List[String]] = {
      q.walkLabels().map(prefix +: _)
    }

    def flattenPrefixedExprs[T](t: T)(implicit q: Queryable.Row[T, _]): Seq[Expr[_]] = {
      q.walkExprs(t)
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
    def toSqlExpr0(implicit ctx: Context) = {
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
