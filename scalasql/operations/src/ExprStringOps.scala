package scalasql.operations

import scalasql.core.Expr
import scalasql.core.SqlStr.SqlStringSyntax

trait ExprStringOps[T] {
  protected def v: Expr[T]

  /** Removes leading and trailing whitespace characters from a character string. */
  def trim: Expr[T] = Expr { implicit ctx => sql"TRIM($v)" }

  /** Removes leading whitespace characters from a character string. */
  def ltrim: Expr[T] = Expr { implicit ctx => sql"LTRIM($v)" }

  /** Removes trailing whitespace characters from a character string. */
  def rtrim: Expr[T] = Expr { implicit ctx => sql"RTRIM($v)" }

  /**
   * The replace(X,Y,Z) function returns a string formed by substituting string Z
   * for every occurrence of string Y in string X
   */
  def replace(y: Expr[T], z: Expr[T]): Expr[T] = Expr { implicit ctx =>
    sql"REPLACE($v, $y, $z)"
  }

}
