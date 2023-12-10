package scalasql.operations
import scalasql.core.Expr
import scalasql.core.SqlStr.SqlStringSyntax

abstract class ExprStringLikeOps[T](v: Expr[T]) {

  /** Concatenates two strings */
  def +(x: Expr[T]): Expr[T] = Expr { implicit ctx => sql"($v || $x)" }

  /** TRUE if the operand matches a pattern */
  def like(x: Expr[T]): Expr[Boolean] = Expr { implicit ctx => sql"($v LIKE $x)" }

  /** Returns an integer value representing the starting position of a string within the search string. */
  def indexOf(x: Expr[T]): Expr[Int]

  /** Converts a string to all lowercase characters. */
  def toLowerCase: Expr[T] = Expr { implicit ctx => sql"LOWER($v)" }

  /** Converts a string to all uppercase characters. */
  def toUpperCase: Expr[T] = Expr { implicit ctx => sql"UPPER($v)" }

  /** Returns the number of characters in this string */
  def length: Expr[Int] = Expr { implicit ctx => sql"LENGTH($v)" }

  /** Returns the number of bytes in this string */
  def octetLength: Expr[Int] = Expr { implicit ctx => sql"OCTET_LENGTH($v)" }

  /** Returns a portion of a string. */
  def substring(start: Expr[Int], length: Expr[Int]): Expr[T] = Expr { implicit ctx =>
    sql"SUBSTRING($v, $start, $length)"
  }

  /** Returns whether or not this strings starts with the other. */
  def startsWith(other: Expr[T]): Expr[Boolean] = Expr { implicit ctx =>
    sql"($v LIKE $other || '%')"
  }

  /** Returns whether or not this strings ends with the other. */
  def endsWith(other: Expr[T]): Expr[Boolean] = Expr { implicit ctx =>
    sql"($v LIKE '%' || $other)"
  }

  /** Returns whether or not this strings contains the other. */
  def contains(other: Expr[T]): Expr[Boolean] = Expr { implicit ctx =>
    sql"($v LIKE '%' || $other || '%')"
  }

}
