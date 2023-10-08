package usql
import scala.language.experimental.macros
import OptionPickler.{Reader, Writer}
import usql.Query.TableRef
import usql.SqlStr.SqlStringSyntax

abstract class Table[V[_[_]] <: Product]()(implicit name: sourcecode.Name) extends Table.Base {
  val tableName = name.value
  implicit def self: Table[V] = this

  def metadata: Table.Metadata[V]

  def initMetadata[V[_[_]] <: Product](): Table.Metadata[V] = macro Table.Metadata.applyImpl[V]

  implicit def containerQr: Queryable[V[Expr], V[Val]] =  metadata.queryable

  def query: Query[V[Expr]] = metadata.query()
}

object Table{
  trait Base {
    def tableName: String
  }

  class Metadata[V[_[_]]](val queryable: Queryable[V[Expr], V[Val]],
                          val query: () => Query[V[Expr]])

  object Metadata{

    def applyImpl[V[_[_]] <: Product](c: scala.reflect.macros.blackbox.Context)
                                     ()
                                     (implicit wtt: c.WeakTypeTag[V[Any]]): c.Expr[Metadata[V]] = {
      import c.universe._

      val tableRef = TermName(c.freshName("tableRef"))
      val applyParameters = c.prefix.actualType.member(TermName("apply")).info.paramLists.head

      val queryParams = for(applyParam <- applyParameters) yield {
        val name = applyParam.name
        if (c.prefix.actualType.member(name) != NoSymbol){
          q"${c.prefix}.${TermName(name.toString)}.expr($tableRef)"
        }else{
          q"_root_.usql.Column[${applyParam.info.typeArgs.head}]()(${name.toString}, ${c.prefix}).expr($tableRef)"
        }
      }

      val flattenExprs = for(applyParam <- applyParameters) yield {
        val name = applyParam.name

        q"usql.Table.Internal.flattenPrefixed(t.${TermName(name.toString)}, ${name.toString})"
      }

      val allFlattenedExprs = flattenExprs.reduceLeft((l, r) => q"$l ++ $r")

      c.Expr[Metadata[V]](
        q"""
        new _root_.usql.Table.Metadata[$wtt](
          new usql.Table.Internal.TableQueryable(
            t => $allFlattenedExprs,
            _root_.usql.OptionPickler.macroR
          ),
          () => {
            val $tableRef = new usql.Query.TableRef(this)
            _root_.usql.Query.fromTable(new $wtt(..$queryParams), $tableRef)
          }
        )
       """
      )
    }
  }

  object Internal{
    class TableQueryable[Q, R](flatten0: Q => Seq[(List[String], Expr[_])],
                               valueReader0: Reader[R]) extends Queryable[Q, R] {
      def walk(q: Q): Seq[(List[String], Expr[_])] = flatten0(q)
      def valueReader: Reader[R] = valueReader0
    }

    def flattenPrefixed[T](t: T, prefix: String)
                          (implicit q: Queryable[T, _]): Seq[(List[String], Expr[_])] = {
      q.walk(t).map { case (k, v) => (prefix +: k, v) }
    }
  }
}

case class Column[T]()(implicit val name: sourcecode.Name,
                       val table: Table.Base) {
  def expr(tableRef: TableRef): Expr[T] = new Expr[T] {
    def toSqlExpr0(implicit ctx: QueryToSql.Context) = {
      SqlStr.raw(ctx.fromNaming(tableRef)) +
        usql"." +
        SqlStr.raw(ctx.columnNameMapper(name.value))
    }
  }
}