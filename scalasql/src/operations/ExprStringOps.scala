package scalasql.operations

import scalasql.query.Expr
import scalasql.renderer.SqlStr.SqlStringSyntax

abstract class ExprStringOps(v: Expr[String]) {

  /** Concatenates two strings */
  def +(x: Expr[String]): Expr[String] = Expr { implicit ctx => sql"($v || $x)" }

  /** TRUE if the operand matches a pattern */
  def like[T](x: Expr[T]): Expr[Boolean] = Expr { implicit ctx => sql"($v LIKE $x)" }

  /** Returns an integer value representing the starting position of a string within the search string. */
  def indexOf(x: Expr[String]): Expr[Int]

  /** Converts a string to all lowercase characters. */
  def toLowerCase: Expr[String] = Expr { implicit ctx => sql"LOWER($v)" }

  /** Converts a string to all uppercase characters. */
  def toUpperCase: Expr[String] = Expr { implicit ctx => sql"UPPER($v)" }

  /** Removes leading and trailing whitespace characters from a character string. */
  def trim: Expr[String] = Expr { implicit ctx => sql"TRIM($v)" }

  /** Returns the number of characters in this string */
  def length: Expr[Int] = Expr { implicit ctx => sql"LENGTH($v)" }

  /** Returns the number of bytes in this string */
  def octetLength: Expr[Int] = Expr { implicit ctx => sql"OCTET_LENGTH($v)" }

  /** Removes leading whitespace characters from a character string. */
  def ltrim: Expr[String] = Expr { implicit ctx => sql"LTRIM($v)" }

  /** Removes trailing whitespace characters from a character string. */
  def rtrim: Expr[String] = Expr { implicit ctx => sql"RTRIM($v)" }

  /** Returns a portion of a string. */
  def substring(start: Expr[Int], length: Expr[Int]): Expr[String] = Expr { implicit ctx =>
    sql"SUBSTRING($v, $start, $length)"
  }

  /** Returns whether or not this strings starts with the other. */
  def startsWith(other: Expr[String]): Expr[Boolean] = Expr { implicit ctx =>
    sql"($v LIKE $other || '%')"
  }

  /** Returns whether or not this strings ends with the other. */
  def endsWith(other: Expr[String]): Expr[Boolean] = Expr { implicit ctx =>
    sql"($v LIKE '%' || $other)"
  }

  /** Returns whether or not this strings contains the other. */
  def contains(other: Expr[String]): Expr[Boolean] = Expr { implicit ctx =>
    sql"($v LIKE '%' || $other || '%')"
  }

  /** Returns the result of replacing a substring of one string with another. */
  // Not supported by SQlite
//  def overlay(replacement: Expr[String], start: Expr[Int], length: Expr[Int] = null): Expr[String] = Expr { implicit ctx =>
//    val lengthStr = if (length == null) sql"" else sql" FOR $length"
//    sql"OVERLAY($v PLACING $replacement FROM $start$lengthStr)"
//  }
}
