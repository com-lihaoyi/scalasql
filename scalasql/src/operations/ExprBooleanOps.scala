package scalasql.operations

import scalasql.query.Expr
import scalasql.renderer.SqlStr.SqlStringSyntax

class ExprBooleanOps(v: Expr[Boolean]) {

  /** TRUE if both Boolean expressions are TRUE */
  def &&(x: Expr[Boolean]): Expr[Boolean] = Expr { implicit ctx => sql"$v AND $x" }

  /** TRUE if either Boolean expression is TRUE */
  def ||(x: Expr[Boolean]): Expr[Boolean] = Expr { implicit ctx => sql"$v OR $x" }

  /** Reverses the value of any other Boolean operator */
  def unary_! : Expr[Boolean] = Expr { implicit ctx => sql"NOT $v" }
}
