package scalasql.operations

import scalasql.core.Db
import scalasql.core.SqlStr.SqlStringSyntax

trait DbStringOps[T] {
  protected def v: Db[T]

  /** Removes leading and trailing whitespace characters from a character string. */
  def trim: Db[T] = Db { implicit ctx => sql"TRIM($v)" }

  /** Removes leading whitespace characters from a character string. */
  def ltrim: Db[T] = Db { implicit ctx => sql"LTRIM($v)" }

  /** Removes trailing whitespace characters from a character string. */
  def rtrim: Db[T] = Db { implicit ctx => sql"RTRIM($v)" }

  /**
   * The replace(X,Y,Z) function returns a string formed by substituting string Z
   * for every occurrence of string Y in string X
   */
  def replace(y: Db[T], z: Db[T]): Db[T] = Db { implicit ctx =>
    sql"REPLACE($v, $y, $z)"
  }

}
