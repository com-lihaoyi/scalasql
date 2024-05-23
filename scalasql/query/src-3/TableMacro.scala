package scalasql.query

import scalasql.core.{DialectTypeMappers, Expr => SqlExpr, Queryable, Sc, TypeMapper}
import scala.compiletime.summonInline
import scala.quoted.*

object TableMacros {
  def applyImpl[V[_[_]] <: Product](using Quotes, Type[V]): Expr[Table.Metadata[V]] = {
    import quotes.reflect.*

    val caseClassType = TypeRepr.of[V]
    val constructor = caseClassType.typeSymbol.primaryConstructor
    val constructorTypeParams = constructor.paramSymss(0)
    val constructorValueParams = constructor.paramSymss(1)

    def paramType(param: Symbol): TypeRepr = caseClassType.memberType(param)

    def isTypeParamType(param: Symbol): Boolean =
      paramType(param).typeSymbol.toString != constructorTypeParams.head.toString

    def subParam(paramInfo: TypeRepr, tpe: TypeRepr): TypeRepr =
      paramInfo.substituteTypes(List(constructorTypeParams.head), List(tpe))

    val queryables = '{ (dialect: DialectTypeMappers, n: Int) =>
      {
        @annotation.nowarn("msg=unused")
        given d: DialectTypeMappers = dialect

        ${
          Expr.ofList(constructorValueParams.map { param =>
            val paramTpe = paramType(param)
            val tpe = subParam(paramTpe, TypeRepr.of[Sc])
            val tpe2 = subParam(paramTpe, TypeRepr.of[SqlExpr])
            (tpe.asType, tpe2.asType) match {
              case ('[t], '[t2]) => '{ summonInline[Queryable.Row[t2, t]] }
            }
          })
        }.apply(n)
      }
    }

    val walkLabels0 = '{ () =>
      ${
        Expr.ofList(constructorValueParams.map { param =>
          if (isTypeParamType(param))
            paramType(param) match {
              case AppliedType(tpeCtor, _) =>
                tpeCtor.asType match {
                  case '[
                      type t[_[_]]; t] =>
                    '{ summonInline[Table.ImplicitMetadata[t]].value.walkLabels0() }
                }
            }
          else '{ Seq(${ Expr(param.name) }) }
        })
      }.flatten
    }

    val queryable = '{
      (
          walkLabels0: () => Seq[String],
          dialect: DialectTypeMappers,
          queryable: Table.Metadata.QueryableProxy
      ) =>
        new Table.Internal.TableQueryable(
          walkLabels0,
          (table: V[SqlExpr]) =>
            ${
              Expr.ofList(constructorValueParams.zipWithIndex.map { case (param, i) =>
                val paramTpe = paramType(param)
                val tpe = subParam(paramTpe, TypeRepr.of[Sc])
                val tpe2 = subParam(paramTpe, TypeRepr.of[SqlExpr])
                (tpe.asType, tpe2.asType) match {
                  case ('[t], '[t2]) =>
                    '{
                      queryable[t2, t](${ Expr(i) }).walkExprs(
                        ${ Select.unique('table.asTerm, param.name).asExprOf[t2] }
                      )
                    }
                }
              })
            }.flatten,
          construct0 = (args: Queryable.ResultSetIterator) =>
            ${
              Apply(
                TypeApply(
                  Select.unique(New(TypeIdent(TypeRepr.of[V].typeSymbol)), "<init>"),
                  List(TypeTree.of[Sc])
                ),
                constructorValueParams.zipWithIndex.map { case (param, i) =>
                  val paramTpe = paramType(param)
                  val tpe = subParam(paramTpe, TypeRepr.of[Sc]).asType
                  val tpe2 = subParam(paramTpe, TypeRepr.of[SqlExpr]).asType

                  (tpe, tpe2) match {
                    case ('[t], '[t2]) => '{ queryable[t2, t](${ Expr(i) }).construct(args) }.asTerm
                  }
                }
              ).asExprOf[V[Sc]]
            },
          deconstruct0 = (r: V[Sc]) =>
            ${
              Apply(
                TypeApply(
                  Select.unique(New(TypeIdent(TypeRepr.of[V].typeSymbol)), "<init>"),
                  List(TypeTree.of[SqlExpr])
                ),
                constructorValueParams.zipWithIndex.map { case (param, i) =>
                  val paramTpe = paramType(param)
                  val tpe = subParam(paramTpe, TypeRepr.of[Sc]).asType
                  val tpe2 = subParam(paramTpe, TypeRepr.of[SqlExpr]).asType

                  (tpe, tpe2) match {
                    case ('[t], '[t2]) =>
                      '{
                        queryable[t2, t](${ Expr(i) }).deconstruct(
                          ${ Select.unique('r.asTerm, param.name).asExprOf[t] }
                        )
                      }.asTerm
                  }
                }
              ).asExprOf[V[SqlExpr]]
            }
        )
    }

    val vExpr0 = '{
      (tableRef: TableRef, dialect: DialectTypeMappers, queryable: Table.Metadata.QueryableProxy) =>
        {
          @annotation.nowarn("msg=unused")
          given d: DialectTypeMappers = dialect

          ${
            Apply(
              TypeApply(
                Select.unique(New(TypeIdent(TypeRepr.of[V].typeSymbol)), "<init>"),
                List(TypeTree.of[Column])
              ),
              constructorValueParams.map { param =>
                val paramTpe = paramType(param)

                if (isTypeParamType(param))
                  paramTpe match {
                    case AppliedType(tpeCtor, _) =>
                      tpeCtor.asType match {
                        case '[
                            type t[_[_]]; t] =>
                          '{
                            summonInline[Table.ImplicitMetadata[t]].value.vExpr(tableRef, dialect)
                          }.asTerm
                      }
                  }
                else
                  paramTpe.typeArgs.head.asType match {
                    case '[t] =>
                      '{
                        new Column[t](
                          tableRef,
                          Table.columnNameOverride(tableRef.value)(${ Expr(param.name) })
                        )(using summonInline[TypeMapper[t]])
                      }.asTerm
                  }
              }
            ).asExprOf[V[Column]]
          }
        }
    }

    '{ new Table.Metadata[V]($queryables, $walkLabels0, $queryable, $vExpr0) }
  }
}

trait TableMacros {
  inline given initTableMetadata[V[_[_]] <: Product]: Table.Metadata[V] = ${
    TableMacros.applyImpl[V]
  }
}
