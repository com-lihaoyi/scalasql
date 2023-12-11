package scalasql.operations

import scalasql.core.Aggregatable
import scalasql.core.{TypeMapper, Expr}

abstract class ExprAggOps[T](v: Aggregatable[Expr[T]]) {

  /** Concatenates the given values into one string using the given separator */
  def mkString(sep: Expr[String] = null)(implicit tm: TypeMapper[T]): Expr[String]
}
