package scalasql.utils
import scalasql.Table
import scala.language.experimental.macros

object TableMacros {
  def applyImpl[V[_[_]]](
      c: scala.reflect.macros.blackbox.Context
  )()(self: c.Expr[Table[V]])(implicit caseClassType: c.WeakTypeTag[V[Any]]): c.Expr[Unit] = {
    import c.universe._

    val tableRef = TermName(c.freshName("tableRef"))
    val applyMethod = c.prefix.actualType.member(TermName("apply"))
    val applyParameters = applyMethod.info.paramLists.head

    val columnParams = for (applyParam <- applyParameters) yield {
      val name = applyParam.name
      if (applyParam.info.typeSymbol.companion != NoSymbol){
        val companion = applyParam.info.typeSymbol.companion
        q"_root_.scalasql.Table.tableMetadata($companion).vExpr($tableRef, dialect)"

      }else {
        q"""
          _root_.scalasql.Column[${applyParam.info.typeArgs.head}]()(
            implicitly,
            sourcecode.Name(_root_.scalasql.Table.tableColumnNameOverride(tableSelf)(${name.toString})),
            ${c.prefix}
          ).expr($tableRef)
        """
      }
    }

    def subApplyParam(applyParam: Symbol) = {
      applyParam.info.substituteTypes(
        List(applyMethod.info.typeParams.head),
        List(typeOf[scalasql.Id[_]].asInstanceOf[ExistentialType].underlying.asInstanceOf[TypeRef].sym.info)
      )
    }
    val constructParams = for (applyParam <- applyParameters) yield {
      val tpe = subApplyParam(applyParam)
      q"implicitly[_root_.scalasql.Queryable.Row[_, $tpe]].construct(args): scalasql.Id[$tpe]"
    }

    val deconstructParams = for ((applyParam, i) <- applyParameters.zipWithIndex) yield {
      val tpe = subApplyParam(applyParam)
      q"(v: Any) => implicitly[_root_.scalasql.Queryable.Row[_, $tpe]].deconstruct(v.asInstanceOf[$tpe])"
    }

    val flattenLists = for (applyParam <- applyParameters) yield {
      if (applyParam.info.typeSymbol.companion != NoSymbol) {
        val companion = applyParam.info.typeSymbol.companion
        q"_root_.scalasql.Table.tableLabels($companion).map(List(_))"
      }else {
        val name = applyParam.name
        q"_root_.scala.List(List(${name.toString}))"
      }
    }

    val flattenExprs = for (applyParam <- applyParameters) yield {
      val name = applyParam.name
      q"_root_.scalasql.Table.Internal.flattenPrefixedExprs(table.${TermName(name.toString)})"
    }

    c.Expr[Unit](q"""
    import _root_.scalasql.renderer.SqlStr.SqlStringSyntax
    _root_.scalasql.Table.setTableMetadata0(
      $self,
      new _root_.scalasql.Table.Metadata[$caseClassType](
        () => ${flattenLists.reduceLeft((l, r) => q"$l ++ $r")},
        dialect => {
          import dialect._
          new _root_.scalasql.Table.Internal.TableQueryable(
            () => ${flattenLists.reduceLeft((l, r) => q"$l ++ $r")},
            table => ${flattenExprs.reduceLeft((l, r) => q"$l ++ $r")},
            construct0 = args => new $caseClassType(..$constructParams),
            deconstruct0 = Seq(..$deconstructParams)
          )
        },
        ($tableRef: _root_.scalasql.query.TableRef, dialect) => {
          import dialect._
          new $caseClassType(..$columnParams)
        }
      )
    )
    """)
  }

}
trait TableMacros {
  def initTableMetadata[V[_[_]]]()(implicit self: Table[V]): Unit = macro TableMacros.applyImpl[V]
}
