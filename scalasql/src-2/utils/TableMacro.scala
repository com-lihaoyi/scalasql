package scalasql.utils
import scalasql.{Id, Table}
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

    def subParam(paramInfo: Type, tpe: Type) = {
      paramInfo.substituteTypes(
        List(constructor.info.resultType.typeArgs.head.typeSymbol),
        List(
          tpe
            .asInstanceOf[ExistentialType]
            .underlying
            .asInstanceOf[TypeRef]
            .sym
            .info
        )
      )
    }

    val queryables = for (param <- constructorParameters) yield {
      val tpe = subParam(param.info, typeOf[Id[_]])
      val tpe2 = subParam(param.info, typeOf[scalasql.Expr[_]])
      q"implicitly[_root_.scalasql.Queryable.Row[$tpe2, $tpe]]"
    }

    val constructParams = for ((param, i) <- constructorParameters.zipWithIndex) yield {
      val tpe = subParam(param.info, typeOf[Id[_]])
      val tpe2 = subParam(param.info, typeOf[scalasql.Expr[_]])
      q"queryable[$tpe2, $tpe]($i).construct(args): _root_.scalasql.Id[$tpe]"
    }

    val deconstructParams = for ((param, i) <- constructorParameters.zipWithIndex) yield {
      val tpe = subParam(param.info, typeOf[Id[_]])
      val tpe2 = subParam(param.info, typeOf[scalasql.Expr[_]])
      q"queryable[$tpe2, $tpe]($i).deconstruct(r.${TermName(param.name.toString)})"
    }

    val flattenLists = for (param <- constructorParameters) yield {
      if (isTypeParamType(param)) {
        q"implicitly[scalasql.Table.ImplicitMetadata[${param.info.typeSymbol}]].value.walkLabels0()"
      } else {
        val name = param.name
        q"_root_.scala.List(${name.toString})"
      }
    }

    val flattenExprs = for ((param, i) <- constructorParameters.zipWithIndex) yield {
      val tpe = subParam(param.info, typeOf[Id[_]])
      val tpe2 = subParam(param.info, typeOf[scalasql.Expr[_]])
      q"queryable[$tpe2, $tpe]($i).walkExprs(table.${TermName(param.name.toString)})"
    }

    import compat._
    val typeRef = caseClassType.tpe.resultType.asInstanceOf[TypeRef]
    val exprRef = TypeRef(
      pre = typeRef.pre,
      sym = typeRef.sym,
      args = weakTypeOf[V[scalasql.Expr]].typeArgs
    )
    val idRef = TypeRef(
      pre = typeRef.pre,
      sym = typeRef.sym,
      args = weakTypeOf[V[scalasql.Id]].typeArgs
    )
    c.Expr[Metadata[V]](q"""{

    new _root_.scalasql.Table.Metadata(
      (dialect, n) => {
        import dialect._;
        n match{ case ..${queryables.zipWithIndex.map { case (q, i) => cq"$i => $q" }} }
      },
      () => ${flattenLists.reduceLeft((l, r) => q"$l ++ $r")},
      (walkLabels0, dialect, queryable) => {
        import dialect._

        new _root_.scalasql.Table.Internal.TableQueryable(
          walkLabels0,
          (table: $exprRef) => ${flattenExprs.reduceLeft((l, r) => q"$l ++ $r")},
          construct0 = (args: _root_.scalasql.Queryable.ResultSetIterator) => new $caseClassType(..$constructParams),
          deconstruct0 = (r: $idRef) => new $caseClassType(..$deconstructParams)
        )
      },
      ($tableRef: _root_.scalasql.query.TableRef, dialect, queryable) => {
        import dialect._

        new $caseClassType(..$columnParams)
      }
    )
    }""")
  }

}
trait TableMacros {
  implicit def initTableMetadata[V[_[_]]]: Metadata[V] = macro TableMacros.applyImpl[V]
}
