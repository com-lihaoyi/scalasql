package usql.operations

import usql.query.Expr
import usql.renderer.SqlStr.SqlStringSyntax

class ExprBooleanOps(v: Expr[Boolean]) {
  /** TRUE if both Boolean expressions are TRUE */
  def &&(x: Expr[Boolean]): Expr[Boolean] = Expr { implicit ctx => usql"$v AND $x"}

  /** TRUE if either Boolean expression is TRUE */
  def ||(x: Expr[Boolean]): Expr[Boolean] = Expr { implicit ctx => usql"$v OR $x"}

  /** Reverses the value of any other Boolean operator */
  def unary_! : Expr[Boolean] = Expr { implicit ctx => usql"NOT $v"}
}