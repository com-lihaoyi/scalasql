package scalasql.operations

import scalasql.core.Db
import scalasql.core.SqlStr.SqlStringSyntax

abstract class DbStringOps(v: Db[String]) {

  /** Concatenates two strings */
  def +(x: Db[String]): Db[String] = Db { implicit ctx => sql"($v || $x)" }

  /** TRUE if the operand matches a pattern */
  def like[T](x: Db[T]): Db[Boolean] = Db { implicit ctx => sql"($v LIKE $x)" }

  /** Returns an integer value representing the starting position of a string within the search string. */
  def indexOf(x: Db[String]): Db[Int]

  /** Converts a string to all lowercase characters. */
  def toLowerCase: Db[String] = Db { implicit ctx => sql"LOWER($v)" }

  /** Converts a string to all uppercase characters. */
  def toUpperCase: Db[String] = Db { implicit ctx => sql"UPPER($v)" }

  /** Removes leading and trailing whitespace characters from a character string. */
  def trim: Db[String] = Db { implicit ctx => sql"TRIM($v)" }

  /** Returns the number of characters in this string */
  def length: Db[Int] = Db { implicit ctx => sql"LENGTH($v)" }

  /** Returns the number of bytes in this string */
  def octetLength: Db[Int] = Db { implicit ctx => sql"OCTET_LENGTH($v)" }

  /** Removes leading whitespace characters from a character string. */
  def ltrim: Db[String] = Db { implicit ctx => sql"LTRIM($v)" }

  /** Removes trailing whitespace characters from a character string. */
  def rtrim: Db[String] = Db { implicit ctx => sql"RTRIM($v)" }

  /** Returns a portion of a string. */
  def substring(start: Db[Int], length: Db[Int]): Db[String] = Db { implicit ctx =>
    sql"SUBSTRING($v, $start, $length)"
  }

  /** Returns whether or not this strings starts with the other. */
  def startsWith(other: Db[String]): Db[Boolean] = Db { implicit ctx =>
    sql"($v LIKE $other || '%')"
  }

  /** Returns whether or not this strings ends with the other. */
  def endsWith(other: Db[String]): Db[Boolean] = Db { implicit ctx =>
    sql"($v LIKE '%' || $other)"
  }

  /** Returns whether or not this strings contains the other. */
  def contains(other: Db[String]): Db[Boolean] = Db { implicit ctx =>
    sql"($v LIKE '%' || $other || '%')"
  }

  /** Returns the result of replacing a substring of one string with another. */
  // Not supported by SQlite
//  def overlay(replacement: Db[String], start: Db[Int], length: Db[Int] = null): Db[String] = Db { implicit ctx =>
//    val lengthStr = if (length == null) sql"" else sql" FOR $length"
//    sql"OVERLAY($v PLACING $replacement FROM $start$lengthStr)"
//  }
}
