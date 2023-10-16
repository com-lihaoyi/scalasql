package usql.operations

import usql.query.{Expr, Select}
import usql.renderer.SqlStr.SqlStringSyntax


class SelectOps[T](v: Select[T]){

  def contains(other: Expr[_]): Expr[Boolean] = Expr { implicit ctx => usql"$other in $v" }

  def isEmpty: Expr[Boolean] = Expr { implicit ctx => usql"NOT EXISTS $v" }

  def nonEmpty: Expr[Boolean] = Expr { implicit ctx => usql"EXISTS $v" }

}