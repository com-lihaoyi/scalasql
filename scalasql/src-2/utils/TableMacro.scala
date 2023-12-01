package scalasql.utils
import scalasql.Table
import scalasql.Table.Metadata

import scala.language.experimental.macros

object TableMacros {
  def cast[T](x: Any): T = x.asInstanceOf[T]
  def applyImpl[V[_[_]]](
      c: scala.reflect.macros.blackbox.Context
  )(implicit caseClassType: c.WeakTypeTag[V[Any]]): c.Expr[Metadata[V]] = {
    import c.universe._

    val tableRef = TermName(c.freshName("tableRef"))
    val constructor = weakTypeOf[V[Any]].members.find(_.isConstructor).head
    val constructorParameters = constructor.info.paramLists.head

    def isTypeParamType(param: Symbol) = {
      param.info.typeSymbol.toString != caseClassType.tpe.typeParams.head.toString
    }

    val columnParams = for (param <- constructorParameters) yield {
      val name = param.name

      if (isTypeParamType(param)) {
        q"implicitly[scalasql.Table.ImplicitMetadata[${param.info.typeSymbol}]].value.vExpr($tableRef, dialect)"
      } else {
        q"""
          _root_.scalasql.Column[${param.info.typeArgs.head}]()(
            implicitly,
            sourcecode.Name(
              _root_.scalasql.Table.tableColumnNameOverride(
                $tableRef.value.asInstanceOf[scalasql.Table[$caseClassType]]
              )(${name.toString})
            ),
            $tableRef.value
          ).expr($tableRef)
        """
      }
    }

    def subParamId(paramInfo: Type) = {

      paramInfo.substituteTypes(
        List(constructor.info.resultType.typeArgs.head.typeSymbol),
        List(
          typeOf[scalasql.Id[_]]
            .asInstanceOf[ExistentialType]
            .underlying
            .asInstanceOf[TypeRef]
            .sym
            .info
        )
      )
    }
    def subParamExpr(paramInfo: Type) = {

      paramInfo.substituteTypes(
        List(constructor.info.resultType.typeArgs.head.typeSymbol),
        List(
          typeOf[scalasql.Expr[_]]
            .asInstanceOf[ExistentialType]
            .underlying
            .asInstanceOf[TypeRef]
            .sym
            .info
        )
      )
    }

    val queryables = for (param <- constructorParameters) yield {
      val tpe = subParamId(param.info)
      val tpe2 = subParamExpr(param.info)
      q"implicitly[_root_.scalasql.Queryable.Row[$tpe2, $tpe]]"
    }
    val constructParams = for ((param, i) <- constructorParameters.zipWithIndex) yield {
      val tpe = subParamId(param.info)
      q"${queryables(i)}.construct(args): scalasql.Id[$tpe]"
    }

    val deconstructParams = for ((param, i) <- constructorParameters.zipWithIndex) yield {
      val tpe = subParamId(param.info)
      val name = param.name
      q"${queryables(i)}.deconstruct(r.${TermName(name.toString)})"
    }

    val flattenLists = for (param <- constructorParameters) yield {
      if (isTypeParamType(param)) {
        q"implicitly[scalasql.Table.ImplicitMetadata[${param.info.typeSymbol}]].value.walkLabels0()"
      } else {
        val name = param.name
        q"_root_.scala.List(List(${name.toString}))"
      }
    }

    val flattenExprs = for ((param, i) <- constructorParameters.zipWithIndex) yield {
      val name = param.name
      q"${queryables(i)}.walkExprs(table.${TermName(name.toString)})"
    }

    import compat._
    val newRef = TypeRef(
      pre = caseClassType.tpe.resultType.asInstanceOf[TypeRef].pre,
      sym = caseClassType.tpe.resultType.asInstanceOf[TypeRef].sym,
      args = weakTypeOf[V[scalasql.Expr]].typeArgs
    )
    c.Expr[Metadata[V]](q"""
    import _root_.scalasql.renderer.SqlStr.SqlStringSyntax

    new _root_.scalasql.Table.Metadata[$caseClassType](
      () => ${flattenLists.reduceLeft((l, r) => q"$l ++ $r")},
      dialect => {
        import dialect._
        new _root_.scalasql.Table.Internal.TableQueryable(
          () => ${flattenLists.reduceLeft((l, r) => q"$l ++ $r")},
          (table: $newRef) => ${flattenExprs.reduceLeft((l, r) => q"$l ++ $r")},
          construct0 = args => new $caseClassType(..$constructParams),
          deconstruct0 = r => new $caseClassType(..$deconstructParams)
        )
      },
      ($tableRef: _root_.scalasql.query.TableRef, dialect) => {
        import dialect._
        new $caseClassType(..$columnParams)
      }
    )
    """)
  }

}
trait TableMacros {
  implicit def initTableMetadata[V[_[_]]]: Metadata[V] = macro TableMacros.applyImpl[V]
}
