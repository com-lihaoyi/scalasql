package usql
import scala.language.experimental.macros
import OptionPickler.{Reader, Writer}

abstract class Table[V[_[_]] <: Product]()(implicit name: sourcecode.Name) extends Table.Base {
  val tableName = name.value
  implicit def self: Table[V] = this

  def metadata: Table.Metadata[V]

  def initMetadata[V[_[_]] <: Product](): Table.Metadata[V] = macro Table.Metadata.applyImpl[V]

  implicit def containerQr: Queryable[V[Expr], V[Val]] = {
    new Queryable[V[Expr], V[Val]] {
      def valueReader = metadata.valueReader
      def queryWriter = metadata.queryWriter
    }
  }

  def query: Query[V[Expr]] = metadata.query()
}

object Table{
  trait Base {
    def tableName: String
  }

  class Metadata[V[_[_]]](val valueReader: Reader[V[Val]],
                          val queryWriter: ExprFlattener[V[Expr]],
                          val query: () => Query[V[Expr]])

  object Metadata{
    private trait Dummy[T[_]] extends Product

    def exprFlattener[T](flatten0: T => Seq[(List[String], Expr[_])]) = {
      new ExprFlattener[T] {
        def flatten(t: T): Seq[(List[String], Expr[_])] = flatten0(t)
      }
    }
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

        q"usql.ExprFlattener.flattenPrefixed(t.${TermName(name.toString)}, ${name.toString})"
      }

      val allFlattenedExprs = flattenExprs.reduceLeft((l, r) => q"$l ++ $r")

      c.Expr[Metadata[V]](
        q"""
        new _root_.usql.Table.Metadata[$wtt](
          _root_.usql.OptionPickler.macroR,
          usql.Table.Metadata.exprFlattener{ t => $allFlattenedExprs },
          () => {
            val $tableRef = new usql.Query.TableRef(this)
            _root_.usql.Query.fromTable(new $wtt(..$queryParams), $tableRef)
          }
        )
       """
      )
    }
  }
}
