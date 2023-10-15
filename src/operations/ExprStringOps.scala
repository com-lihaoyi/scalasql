package usql.operations

import usql.query.Expr
import usql.renderer.SqlStr.SqlStringSyntax


class ExprStringOps(v: Expr[String]) {
  /** TRUE if the operand matches a pattern */
  def like[T](x: Expr[T]): Expr[Boolean] = Expr { implicit ctx => usql"$v LIKE $x" }

  /** Returns an integer value representing the starting position of a string within the search string. */
  // not supported by Sqlite
//  def position(x: Expr[String]): Expr[Int] = Expr { implicit ctx => usql"POSITION($v, $x)" }

  /** Converts a string to all lowercase characters. */
  def toLowerCase: Expr[String] = Expr { implicit ctx => usql"LOWER($v)" }

  /** Converts a string to all uppercase characters. */
  def toUpperCase: Expr[String] = Expr { implicit ctx => usql"UPPER($v)" }

  /** Removes leading characters, trailing characters, or both from a character string. */
  def trim: Expr[String] = Expr { implicit ctx => usql"TRIM($v)" }

  /** Returns a portion of a string. */
  def substring(start: Expr[Int], length: Expr[Int]): Expr[String] = Expr { implicit ctx => usql"SUBSTRING($v, $start, $length)" }

  /** Returns the result of replacing a substring of one string with another. */
  // Not supported by SQlite
//  def overlay(replacement: Expr[String], start: Expr[Int], length: Expr[Int] = null): Expr[String] = Expr { implicit ctx =>
//    val lengthStr = if (length == null) usql"" else usql" FOR $length"
//    usql"OVERLAY($v PLACING $replacement FROM $start$lengthStr)"
//  }
}

