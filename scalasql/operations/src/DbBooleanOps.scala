package scalasql.operations

import scalasql.core.Db
import scalasql.core.SqlStr.SqlStringSyntax

class DbBooleanOps(v: Db[Boolean]) {

  /** TRUE if both Boolean expressions are TRUE */
  def &&(x: Db[Boolean]): Db[Boolean] = Db { implicit ctx => sql"($v AND $x)" }

  /** TRUE if either Boolean expression is TRUE */
  def ||(x: Db[Boolean]): Db[Boolean] = Db { implicit ctx => sql"($v OR $x)" }

  /** Reverses the value of any other Boolean operator */
  def unary_! : Db[Boolean] = Db { implicit ctx => sql"(NOT $v)" }
}
