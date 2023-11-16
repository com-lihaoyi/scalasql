package scalasql.operations

import scalasql.query.{Expr, Select}
import scalasql.renderer.SqlStr.SqlStringSyntax

class SelectOps[T](v: Select[T, _]) {

  /**
   * Returns whether or not the [[Select]] on the left contains the [[other]] value on the right
   */
  def contains(other: Expr[_]): Expr[Boolean] = Expr { implicit ctx => sql"($other IN $v)" }

  /**
   * Returns whether or not the [[Select]] on the left is empty with zero elements
   */
  def isEmpty: Expr[Boolean] = Expr { implicit ctx => sql"(NOT EXISTS $v)" }

  /**
   * Returns whether or not the [[Select]] on the left is nonempty with one or more elements
   */
  def nonEmpty: Expr[Boolean] = Expr { implicit ctx => sql"(EXISTS $v)" }

}
