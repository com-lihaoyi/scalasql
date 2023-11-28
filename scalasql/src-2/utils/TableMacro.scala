package scalasql.utils
import scalasql.Table
import scala.language.experimental.macros

object TableMacros {
  def applyImpl[V[_[_]]](
      c: scala.reflect.macros.blackbox.Context
  )()(self: c.Expr[Table[V]])(implicit wtt: c.WeakTypeTag[V[Any]]): c.Expr[Unit] = {
    import c.universe._

    val tableRef = TermName(c.freshName("tableRef"))
    val applyParameters = c.prefix.actualType.member(TermName("apply")).info.paramLists.head

    val queryParams = for (applyParam <- applyParameters) yield {
      val name = applyParam.name
      q"""
        _root_.scalasql.Column[${applyParam.info.typeArgs.head}]()(
          implicitly,
          sourcecode.Name(_root_.scalasql.Table.tableColumnNameOverride(tableSelf)(${name.toString})),
          ${c.prefix}
        ).expr($tableRef)
      """
    }
    val allTypeMappers = for (applyParam <- applyParameters) yield {
      q"""implicitly[_root_.scalasql.TypeMapper[${applyParam.info.typeArgs.head}]]"""
    }

    val flattenLists = for (applyParam <- applyParameters) yield {
      val name = applyParam.name
      q"_root_.scalasql.Table.Internal.flattenPrefixedLists[_root_.scalasql.Expr[${applyParam.info.typeArgs.head}]](${name.toString})"
    }

    val flattenExprs = for (applyParam <- applyParameters) yield {
      val name = applyParam.name
      q"_root_.scalasql.Table.Internal.flattenPrefixedExprs(table.${TermName(name.toString)})"
    }

    c.Expr[Unit](q"""
    import _root_.scalasql.renderer.SqlStr.SqlStringSyntax
    _root_.scalasql.Table.setTableMetadata0(
      $self,
      new _root_.scalasql.Table.Metadata[$wtt](
        dialect => {
          import dialect._
          new _root_.scalasql.Table.Internal.TableQueryable(
            () => ${flattenLists.reduceLeft((l, r) => q"$l ++ $r")},
            table => ${flattenExprs.reduceLeft((l, r) => q"$l ++ $r")},
            $allTypeMappers,
            _root_.scalasql.utils.OptionPickler.macroR
          )
        },
        ($tableRef: _root_.scalasql.query.TableRef, dialect) => {
          import dialect._
          new $wtt(..$queryParams)
        }
      )
    )
    """)
  }

}
trait TableMacros {
  def initTableMetadata[V[_[_]]]()(implicit self: Table[V]): Unit = macro TableMacros.applyImpl[V]
}
