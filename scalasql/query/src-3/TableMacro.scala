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

    def paramTypes(param: Symbol): (TypeRepr, Type[?], Type[?]) = {
      val paramTpe = paramType(param)
      val scTpe = subParam(paramTpe, TypeRepr.of[Sc]).asType
      val exprTpe = subParam(paramTpe, TypeRepr.of[SqlExpr]).asType
      (paramTpe, scTpe, exprTpe)
    }

    def constructV[F[_]: Type](
        paramTerm: (Symbol, Int) => (TypeRepr, Type[?], Type[?]) => Term
    ): Expr[V[F]] =
      Apply(
        TypeApply(
          Select.unique(New(TypeIdent(TypeRepr.of[V].typeSymbol)), "<init>"),
          List(TypeTree.of[F])
        ),
        constructorValueParams.zipWithIndex.map { case (param, i) =>
          paramTerm(param, i).tupled(paramTypes(param))
        }
      ).asExprOf[V[F]]

    def fromImplicitMetadata[O](paramTpe: TypeRepr)(
        f: [T[_[_]]] => Expr[Table.Metadata[T]] => Type[T] ?=> O
    ): O =
      paramTpe match {
        case AppliedType(tpeCtor, _) =>
          tpeCtor.asType match {
            case '[
                type t[_[_]]; t] =>
              f[t]('{ summonInline[Table.ImplicitMetadata[t]].value })
          }
      }

    val queryables = '{ (dialect: DialectTypeMappers, n: Int) =>
      {
        import dialect.*

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
            fromImplicitMetadata(paramType(param))([T[_[_]]] => t => '{ $t.walkLabels0() })
          else
            '{ Seq(${ Expr(param.name) }) }
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
              constructV[Sc]((_, i) => { case (_, '[t], '[t2]) =>
                '{ queryable[t2, t](${ Expr(i) }).construct(args) }.asTerm
              })
            },
          deconstruct0 = (r: V[Sc]) =>
            ${
              constructV[SqlExpr]((param, i) => { case (_, '[t], '[t2]) =>
                '{
                  queryable[t2, t](${ Expr(i) }).deconstruct(
                    ${ Select.unique('r.asTerm, param.name).asExprOf[t] }
                  )
                }.asTerm
              })
            }
        )
    }

    val vExpr0 = '{
      (tableRef: TableRef, dialect: DialectTypeMappers, queryable: Table.Metadata.QueryableProxy) =>
        {
          import dialect.*

          ${
            constructV[Column]((param, _) => { case (paramTpe, _, _) =>
              if (isTypeParamType(param))
                fromImplicitMetadata(paramTpe)(
                  [T[_[_]]] => t => '{ $t.vExpr(tableRef, dialect) }.asTerm
                )
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
            })
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
