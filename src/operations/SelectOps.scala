package scalasql.operations

import scalasql.query.{Expr, Select}
import scalasql.renderer.SqlStr.SqlStringSyntax

class SelectOps[T](v: Select[T, _]) {

  def contains(other: Expr[_]): Expr[Boolean] = Expr { implicit ctx => sql"$other in $v" }

  def isEmpty: Expr[Boolean] = Expr { implicit ctx => sql"NOT EXISTS $v" }

  def nonEmpty: Expr[Boolean] = Expr { implicit ctx => sql"EXISTS $v" }

}
