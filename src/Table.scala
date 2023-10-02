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
      def toTables(t: V[Expr]): Set[Table.Base] = t.productIterator.map(_.asInstanceOf[Expr[_]]).flatMap(_.toTables).toSet
      def valueReader = metadata.valueReader
      def queryWriter = metadata.queryWriter
    }
  }

  def query: Query[V[Expr]] = metadata.query
}

object Table{
  trait Base {
    def tableName: String
  }

  class Metadata[V[_[_]]](val valueReader: Reader[V[Val]],
                          val queryWriter: Writer[V[Expr]],
                          val query: Query[V[Expr]])

  object Metadata{
    private trait Dummy[T[_]] extends Product
    def applyImpl[V[_[_]] <: Product](c: scala.reflect.macros.blackbox.Context)
                                     ()
                                     (implicit wtt: c.WeakTypeTag[V[Any]]): c.Expr[Metadata[V]] = {
      import c.universe._

      val applyParameters = c.prefix.actualType.member(TermName("apply")).info.paramLists.head

      val queryParams = for(applyParam <- applyParameters) yield {
        val name = applyParam.name
        if (c.prefix.actualType.member(name) != NoSymbol){
          q"${c.prefix}.${TermName(name.toString)}.expr"
        }else{
          q"_root_.usql.Column[${applyParam.info.typeArgs.head}]()(${name.toString}, ${c.prefix}).expr"
        }
      }

      c.Expr[Metadata[V]](
        q"""
        new _root_.usql.Table.Metadata[$wtt](
          _root_.usql.OptionPickler.macroR,
          _root_.usql.OptionPickler.macroW,
          _root_.usql.Query(new $wtt(..$queryParams))
        )
       """
      )
    }
  }
}
