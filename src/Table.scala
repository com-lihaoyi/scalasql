package usql
import scala.language.experimental.macros
import renderer.{Context, SelectToSql, SqlStr}
import usql.query.{Expr, Insert, InsertValues, Joinable, Select, TableRef, Update}
import renderer.SqlStr.SqlStringSyntax
import usql.utils.OptionPickler

abstract class Table[V[_[_]]]()(implicit name: sourcecode.Name)
    extends Table.Base {

  val tableName = name.value
  implicit def self: Table[V] = this

  def metadata: Table.Metadata[V]

  def initMetadata[V[_[_]]](): Table.Metadata[V] = macro Table.Metadata.applyImpl[V]

  implicit def containerQr[E[_] <: Expr[_]]: Queryable[V[E], V[Val]] =
    metadata.queryable.asInstanceOf[Queryable[V[E], V[Val]]]

  def tableRef = new usql.query.TableRef(this)
}

object Table {

  trait Base {
    def tableName: String
  }

  class Metadata[V[_[_]]](
      val queryable: Queryable[V[Expr], V[Val]],
      val vExpr: TableRef => V[Column.ColumnExpr]
  )

  object Metadata {

    def applyImpl[V[_[_]]](c: scala.reflect.macros.blackbox.Context)()(implicit
        wtt: c.WeakTypeTag[V[Any]]
    ): c.Expr[Metadata[V]] = {
      import c.universe._

      val tableRef = TermName(c.freshName("tableRef"))
      val applyParameters = c.prefix.actualType.member(TermName("apply")).info.paramLists.head

      val queryParams = for (applyParam <- applyParameters) yield {
        val name = applyParam.name
        if (c.prefix.actualType.member(name) != NoSymbol) {
          q"${c.prefix}.${TermName(name.toString)}.expr($tableRef)"
        } else {
          q"_root_.usql.Column[${applyParam.info.typeArgs.head}]()(${name.toString}, ${c.prefix}).expr($tableRef)"
        }
      }

      val flattenExprs = for (applyParam <- applyParameters) yield {
        val name = applyParam.name

        q"_root_.usql.Table.Internal.flattenPrefixed(table.${TermName(name.toString)}, ${name.toString})"
      }

      val allFlattenedExprs = flattenExprs.reduceLeft((l, r) => q"$l ++ $r")

      c.Expr[Metadata[V]](
        q"""
        new _root_.usql.Table.Metadata[$wtt](
          new usql.Table.Internal.TableQueryable(
            table => $allFlattenedExprs,
            _root_.usql.utils.OptionPickler.macroR
          ),
          ($tableRef: _root_.usql.query.TableRef) => new $wtt(..$queryParams)
        )
        """
      )
    }
  }

  object Internal {
    class TableQueryable[Q, R](
        flatten0: Q => Seq[(List[String], Expr[_])],
        valueReader0: OptionPickler.Reader[R]
    ) extends Queryable[Q, R] {
      def walk(q: Q): Seq[(List[String], Expr[_])] = flatten0(q)

      override def valueReader(q: Q): OptionPickler.Reader[R] = valueReader0
    }

    def flattenPrefixed[T](t: T, prefix: String)(implicit
        q: Queryable[T, _]
    ): Seq[(List[String], Expr[_])] = {
      q.walk(t).map { case (k, v) => (prefix +: k, v) }
    }
  }
}

case class Column[T]()(implicit val name: sourcecode.Name, val table: Table.Base) {
  def expr(tableRef: TableRef): Column.ColumnExpr[T] =
    new Column.ColumnExpr[T](tableRef, name.value)
}

object Column {
  class ColumnExpr[T](tableRef: TableRef, val name: String) extends Expr[T] {
    def toSqlExpr0(implicit ctx: Context) = {
      val prefix = ctx.fromNaming(tableRef) match {
        case "" => usql""
        case s => SqlStr.raw(s) + usql"."
      }

      prefix + SqlStr.raw(ctx.columnNameMapper(name))
    }
  }
}
