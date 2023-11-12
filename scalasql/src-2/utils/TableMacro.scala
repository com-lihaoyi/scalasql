package scalasql.utils
import scalasql.Table
import scala.language.experimental.macros

object TableMacros{
  def applyImpl[V[_[_]]](
                          c: scala.reflect.macros.blackbox.Context
                        )()(implicit wtt: c.WeakTypeTag[V[Any]]): c.Expr[Table.Metadata[V]] = {
    import c.universe._

    val tableRef = TermName(c.freshName("tableRef"))
    val applyParameters = c.prefix.actualType.member(TermName("apply")).info.paramLists.head

    val queryParams = for (applyParam <- applyParameters) yield {
      val name = applyParam.name
      q"_root_.scalasql.Column[${applyParam.info.typeArgs.head}]()(implicitly, ${name.toString}, ${c.prefix}).expr($tableRef)"
    }

    val flattenExprs = for (applyParam <- applyParameters) yield {
      val name = applyParam.name
      q"_root_.scalasql.Table.Internal.flattenPrefixed(table.${TermName(name.toString)}, ${name.toString})"
    }

    val allFlattenedExprs = flattenExprs.reduceLeft((l, r) => q"$l ++ $r")

    c.Expr[Table.Metadata[V]](
      q"""
    new _root_.scalasql.Table.Metadata[$wtt](
      new _root_.scalasql.Table.Internal.TableQueryable(
        table => $allFlattenedExprs,
        _root_.scalasql.utils.OptionPickler.macroR
      ),
      ($tableRef: _root_.scalasql.query.TableRef) => new $wtt(..$queryParams)
    )
    """)
  }

}
trait TableMacros{
  def initMetadata[V[_[_]]](): Table.Metadata[V] = macro TableMacros.applyImpl[V]
}