package scalasql
import scala.language.experimental.macros
import renderer.{Context, ExprsToSql, JoinsToSql, SqlStr}
import scalasql.query.{Expr, Insert, InsertValues, Joinable, Select, TableRef, Update}
import renderer.SqlStr.SqlStringSyntax
import scalasql.utils.OptionPickler

/**
 * In-code representation of a SQL table, associated with a given `case class` [[V]].
 */
abstract class Table[V[_[_]]]()(implicit name: sourcecode.Name)
    extends Table.Base
    with scalasql.utils.TableMacros {

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
  protected def tableColumnNameOverrides: Map[String, String] = Map()
  protected implicit def tableSelf: Table[V] = this

  protected def tableMetadata: Table.Metadata[V]

  implicit def containerQr[E[_] <: Expr[_]]: Queryable.Row[V[E], V[Id]] = tableMetadata.queryable
    .asInstanceOf[Queryable.Row[V[E], V[Id]]]

  protected def tableRef = new scalasql.query.TableRef(this)


}

object Table {
  def tableMetadata[V[_[_]]](t: Table[V]) = t.tableMetadata
  def tableRef[V[_[_]]](t: Table[V]) = t.tableRef
  def tableName[V[_[_]]](t: Table.Base) = t.tableName
  def tableColumnNameOverrides[V[_[_]]](t: Table[V]) = t.tableColumnNameOverrides
  trait Base {
    protected[scalasql] def tableName: String
  }


  class Metadata[V[_[_]]](
      val queryable: Queryable[V[Expr], V[Id]],
      val vExpr: TableRef => V[Column.ColumnExpr]
  )

  object Internal {
    class TableQueryable[Q, R](
        flatten0: Q => Seq[(List[String], Expr[_])],
        val toTypeMappers0: Q => Seq[TypeMapper[_]],
        valueReader0: OptionPickler.Reader[R]
    ) extends Queryable.Row[Q, R] {
      def walk(q: Q): Seq[(List[String], Expr[_])] = flatten0(q)

      override def valueReader(q: Q): OptionPickler.Reader[R] = valueReader0

      def toSqlStr(q: Q, ctx: Context): SqlStr = {
        val walked = this.walk(q)
        val res = ExprsToSql(walked, sql"", ctx)
        res
      }

      def toTypeMappers(q: Q): Seq[TypeMapper[_]] = toTypeMappers0(q)
    }

    def flattenPrefixed[T](t: T, prefix: String)(
        implicit q: Queryable.Row[T, _]
    ): Seq[(List[String], Expr[_])] = { q.walk(t).map { case (k, v) => (prefix +: k, v) } }
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
        case None => sql"SCALASQL_MISSING_TABLE_${SqlStr.raw(Table.tableName(tableRef.value))}.$suffix"
      }
    }
  }
}
