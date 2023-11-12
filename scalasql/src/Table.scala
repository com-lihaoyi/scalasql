package scalasql
import scala.language.experimental.macros
import renderer.{Context, JoinsToSql, SqlStr}
import scalasql.query.{Expr, Insert, InsertValues, Joinable, Select, TableRef, Update}
import renderer.SqlStr.SqlStringSyntax
import scalasql.utils.OptionPickler

/**
 * In-code representation of a SQL table, associated with a given `case class` [[V]].
 */
abstract class Table[V[_[_]]]()(implicit name: sourcecode.Name)
    extends Table.Base
    with scalasql.utils.TableMacros {

  val tableName = name.value
  implicit def self: Table[V] = this

  def metadata: Table.Metadata[V]

  implicit def containerQr[E[_] <: Expr[_]]: Queryable.Row[V[E], V[Id]] = metadata.queryable
    .asInstanceOf[Queryable.Row[V[E], V[Id]]]

  def tableRef = new scalasql.query.TableRef(this)
}

object Table {

  trait Base {
    def tableName: String
  }

  class Metadata[V[_[_]]](
      val queryable: Queryable[V[Expr], V[Id]],
      val vExpr: TableRef => V[Column.ColumnExpr]
  )

  object Internal {
    class TableQueryable[Q, R](
        flatten0: Q => Seq[(List[String], Expr[_])],
        valueReader0: OptionPickler.Reader[R]
    ) extends Queryable.Row[Q, R] {
      def walk(q: Q): Seq[(List[String], Expr[_])] = flatten0(q)

      override def valueReader(q: Q): OptionPickler.Reader[R] = valueReader0
    }

    def flattenPrefixed[T](t: T, prefix: String)(
        implicit q: Queryable.Row[T, _]
    ): Seq[(List[String], Expr[_])] = { q.walk(t).map { case (k, v) => (prefix +: k, v) } }
  }
}

case class Column[T: MappedType]()(implicit val name: sourcecode.Name, val table: Table.Base) {
  def expr(tableRef: TableRef): Column.ColumnExpr[T] =
    new Column.ColumnExpr[T](tableRef, name.value)
}

object Column {
  case class Assignment[T](column: ColumnExpr[T], value: Expr[T])
  class ColumnExpr[T](tableRef: TableRef, val name: String)(implicit val mappedType: MappedType[T])
      extends Expr[T] {
    def :=(v: Expr[T]) = Assignment(this, v)
    def toSqlExpr0(implicit ctx: Context) = {
      val prefix = ctx.fromNaming.get(tableRef) match {
        case Some("") => sql""
        case Some(s) => SqlStr.raw(s) + sql"."
        case None => sql"SCALASQL_MISSING_TABLE_${SqlStr.raw(tableRef.value.tableName)}."
      }

      prefix + SqlStr.raw(ctx.config.columnNameMapper(name))
    }
  }
}
