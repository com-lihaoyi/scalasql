package scalasql.operations

import scalasql.core.Sql
import scalasql.core.SqlStr.SqlStringSyntax

abstract class ExprStringOps(v: Sql[String]) {

  /** Concatenates two strings */
  def +(x: Sql[String]): Sql[String] = Sql { implicit ctx => sql"($v || $x)" }

  /** TRUE if the operand matches a pattern */
  def like[T](x: Sql[T]): Sql[Boolean] = Sql { implicit ctx => sql"($v LIKE $x)" }

  /** Returns an integer value representing the starting position of a string within the search string. */
  def indexOf(x: Sql[String]): Sql[Int]

  /** Converts a string to all lowercase characters. */
  def toLowerCase: Sql[String] = Sql { implicit ctx => sql"LOWER($v)" }

  /** Converts a string to all uppercase characters. */
  def toUpperCase: Sql[String] = Sql { implicit ctx => sql"UPPER($v)" }

  /** Removes leading and trailing whitespace characters from a character string. */
  def trim: Sql[String] = Sql { implicit ctx => sql"TRIM($v)" }

  /** Returns the number of characters in this string */
  def length: Sql[Int] = Sql { implicit ctx => sql"LENGTH($v)" }

  /** Returns the number of bytes in this string */
  def octetLength: Sql[Int] = Sql { implicit ctx => sql"OCTET_LENGTH($v)" }

  /** Removes leading whitespace characters from a character string. */
  def ltrim: Sql[String] = Sql { implicit ctx => sql"LTRIM($v)" }

  /** Removes trailing whitespace characters from a character string. */
  def rtrim: Sql[String] = Sql { implicit ctx => sql"RTRIM($v)" }

  /** Returns a portion of a string. */
  def substring(start: Sql[Int], length: Sql[Int]): Sql[String] = Sql { implicit ctx =>
    sql"SUBSTRING($v, $start, $length)"
  }

  /** Returns whether or not this strings starts with the other. */
  def startsWith(other: Sql[String]): Sql[Boolean] = Sql { implicit ctx =>
    sql"($v LIKE $other || '%')"
  }

  /** Returns whether or not this strings ends with the other. */
  def endsWith(other: Sql[String]): Sql[Boolean] = Sql { implicit ctx =>
    sql"($v LIKE '%' || $other)"
  }

  /** Returns whether or not this strings contains the other. */
  def contains(other: Sql[String]): Sql[Boolean] = Sql { implicit ctx =>
    sql"($v LIKE '%' || $other || '%')"
  }

  /** Returns the result of replacing a substring of one string with another. */
  // Not supported by SQlite
//  def overlay(replacement: Sql[String], start: Sql[Int], length: Sql[Int] = null): Sql[String] = Sql { implicit ctx =>
//    val lengthStr = if (length == null) sql"" else sql" FOR $length"
//    sql"OVERLAY($v PLACING $replacement FROM $start$lengthStr)"
//  }
}
