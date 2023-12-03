package scalasql.operations

import scalasql.query.Sql
import scalasql.renderer.SqlStr.SqlStringSyntax

class ExprBooleanOps(v: Sql[Boolean]) {

  /** TRUE if both Boolean expressions are TRUE */
  def &&(x: Sql[Boolean]): Sql[Boolean] = Sql { implicit ctx => sql"($v AND $x)" }

  /** TRUE if either Boolean expression is TRUE */
  def ||(x: Sql[Boolean]): Sql[Boolean] = Sql { implicit ctx => sql"($v OR $x)" }

  /** Reverses the value of any other Boolean operator */
  def unary_! : Sql[Boolean] = Sql { implicit ctx => sql"(NOT $v)" }
}
