package scalasql.operations

import scalasql.query.{Aggregatable}
import scalasql.core.SqlStr.SqlStringSyntax
import scalasql.core.{Queryable, TypeMapper, Sql}

abstract class AggExprOps[T](v: Aggregatable[Sql[T]]) {

  /** Concatenates the given values into one string using the given separator */
  def mkString(sep: Sql[String] = null)(implicit tm: TypeMapper[T]): Sql[String]

  /** TRUE if the operand is equal to one of a list of expressions or one or more rows returned by a subquery */
  //    def contains(e: Sql[_]): Sql[Boolean] = v.queryExpr(implicit ctx => sql"ALL($e in $v})")
}
