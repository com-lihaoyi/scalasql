package scalasql.operations

import scalasql.core.TypeMapper
import scalasql.core.Db
import scalasql.core.SqlStr.SqlStringSyntax

class DbNumericOps[T: Numeric](v: Db[T])(implicit val m: TypeMapper[T]) {

  /** Addition */
  def +[V: Numeric](x: Db[V]): Db[T] = Db { implicit ctx => sql"($v + $x)" }

  /** Subtraction */
  def -[V: Numeric](x: Db[V]): Db[T] = Db { implicit ctx => sql"($v - $x)" }

  /** Multiplication */
  def *[V: Numeric](x: Db[V]): Db[T] = Db { implicit ctx => sql"($v * $x)" }

  /** Division */
  def /[V: Numeric](x: Db[V]): Db[T] = Db { implicit ctx => sql"($v / $x)" }

  /** Remainder */
  def %[V: Numeric](x: Db[V]): Db[T] = Db { implicit ctx => sql"MOD($v, $x)" }

  /** Bitwise AND */
  def &[V: Numeric](x: Db[V]): Db[T] = Db { implicit ctx => sql"($v & $x)" }

  /** Bitwise OR */
  def |[V: Numeric](x: Db[V]): Db[T] = Db { implicit ctx => sql"($v | $x)" }

  /** Bitwise XOR */
  def ^[V: Numeric](x: Db[V]): Db[T] = Db { implicit ctx => sql"($v ^ $x)" }

  /** TRUE if the operand is within a range */
  def between(x: Db[Int], y: Db[Int]): Db[Boolean] = Db { implicit ctx =>
    sql"$v BETWEEN $x AND $y"
  }

  /** Unary Positive Operator */
  def unary_+ : Db[T] = Db { implicit ctx => sql"+$v" }

  /** Unary Negation Operator */
  def unary_- : Db[T] = Db { implicit ctx => sql"-$v" }

  /** Unary Bitwise NOT Operator */
  def unary_~ : Db[T] = Db { implicit ctx => sql"~$v" }

  /** Returns the absolute value of a number. */
  def abs: Db[T] = Db { implicit ctx => sql"ABS($v)" }

  /** Returns the remainder of one number divided into another. */
  def mod[V: Numeric](x: Db[V]): Db[T] = Db { implicit ctx => sql"MOD($v, $x)" }

  /** Rounds a noninteger value upwards to the next greatest integer. Returns an integer value unchanged. */
  def ceil: Db[T] = Db { implicit ctx => sql"CEIL($v)" }

  /** Rounds a noninteger value downwards to the next least integer. Returns an integer value unchanged. */
  def floor: Db[T] = Db { implicit ctx => sql"FLOOR($v)" }

  /**
   * The sign(X) function returns -1, 0, or +1 if the argument X is a numeric
   * value that is negative, zero, or positive, respectively. If the argument to sign(X)
   * is NULL or is a string or blob that cannot be losslessly converted into a number,
   * then sign(X) returns NULL.
   */
  def sign: Db[T] = Db { implicit ctx => sql"SIGN($v)" }
}
