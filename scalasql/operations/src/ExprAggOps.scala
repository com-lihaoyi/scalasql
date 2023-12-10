package scalasql.operations

import scalasql.core.Aggregatable
import scalasql.core.SqlStr.SqlStringSyntax
import scalasql.core.{Queryable, TypeMapper, Expr}

abstract class ExprAggOps[T](v: Aggregatable[Expr[T]]) {

  /** Concatenates the given values into one string using the given separator */
  def mkString(sep: Expr[String] = null)(implicit tm: TypeMapper[T]): Expr[String]

  /** TRUE if the operand is equal to one of a list of expressions or one or more rows returned by a subquery */
  //    def contains(e: Expr[_]): Expr[Boolean] = v.queryExpr(implicit ctx => sql"ALL($e in $v})")
}
