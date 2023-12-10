package scalasql.operations
import scalasql.core.Db
import scalasql.core.SqlStr.SqlStringSyntax

abstract class DbStringLikeOps[T](v: Db[T]) {

  /** Concatenates two strings */
  def +(x: Db[T]): Db[T] = Db { implicit ctx => sql"($v || $x)" }

  /** TRUE if the operand matches a pattern */
  def like(x: Db[T]): Db[Boolean] = Db { implicit ctx => sql"($v LIKE $x)" }

  /** Returns an integer value representing the starting position of a string within the search string. */
  def indexOf(x: Db[T]): Db[Int]

  /** Converts a string to all lowercase characters. */
  def toLowerCase: Db[T] = Db { implicit ctx => sql"LOWER($v)" }

  /** Converts a string to all uppercase characters. */
  def toUpperCase: Db[T] = Db { implicit ctx => sql"UPPER($v)" }

  /** Returns the number of characters in this string */
  def length: Db[Int] = Db { implicit ctx => sql"LENGTH($v)" }

  /** Returns the number of bytes in this string */
  def octetLength: Db[Int] = Db { implicit ctx => sql"OCTET_LENGTH($v)" }

  /** Returns a portion of a string. */
  def substring(start: Db[Int], length: Db[Int]): Db[T] = Db { implicit ctx =>
    sql"SUBSTRING($v, $start, $length)"
  }

  /** Returns whether or not this strings starts with the other. */
  def startsWith(other: Db[T]): Db[Boolean] = Db { implicit ctx =>
    sql"($v LIKE $other || '%')"
  }

  /** Returns whether or not this strings ends with the other. */
  def endsWith(other: Db[T]): Db[Boolean] = Db { implicit ctx =>
    sql"($v LIKE '%' || $other)"
  }

  /** Returns whether or not this strings contains the other. */
  def contains(other: Db[T]): Db[Boolean] = Db { implicit ctx =>
    sql"($v LIKE '%' || $other || '%')"
  }

}
