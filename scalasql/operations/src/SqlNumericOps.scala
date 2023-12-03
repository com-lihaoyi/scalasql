package scalasql.operations

import scalasql.core.TypeMapper
import scalasql.core.Sql
import scalasql.core.SqlStr.SqlStringSyntax

class SqlNumericOps[T: Numeric](v: Sql[T])(implicit val m: TypeMapper[T]) {

  /** Addition */
  def +[V: Numeric](x: Sql[V]): Sql[T] = Sql { implicit ctx => sql"($v + $x)" }

  /** Subtraction */
  def -[V: Numeric](x: Sql[V]): Sql[T] = Sql { implicit ctx => sql"($v - $x)" }

  /** Multiplication */
  def *[V: Numeric](x: Sql[V]): Sql[T] = Sql { implicit ctx => sql"($v * $x)" }

  /** Division */
  def /[V: Numeric](x: Sql[V]): Sql[T] = Sql { implicit ctx => sql"($v / $x)" }

  /** Remainder */
  def %[V: Numeric](x: Sql[V]): Sql[T] = Sql { implicit ctx => sql"MOD($v, $x)" }

  /** Bitwise AND */
  def &[V: Numeric](x: Sql[V]): Sql[T] = Sql { implicit ctx => sql"($v & $x)" }

  /** Bitwise OR */
  def |[V: Numeric](x: Sql[V]): Sql[T] = Sql { implicit ctx => sql"($v | $x)" }

  /** Bitwise XOR */
  def ^[V: Numeric](x: Sql[V]): Sql[T] = Sql { implicit ctx => sql"($v ^ $x)" }

  /** TRUE if the operand is within a range */
  def between(x: Sql[Int], y: Sql[Int]): Sql[Boolean] = Sql { implicit ctx =>
    sql"$v BETWEEN $x AND $y"
  }

  /** Unary Positive Operator */
  def unary_+ : Sql[T] = Sql { implicit ctx => sql"+$v" }

  /** Unary Negation Operator */
  def unary_- : Sql[T] = Sql { implicit ctx => sql"-$v" }

  /** Unary Bitwise NOT Operator */
  def unary_~ : Sql[T] = Sql { implicit ctx => sql"~$v" }

  /** Returns the absolute value of a number. */
  def abs: Sql[T] = Sql { implicit ctx => sql"ABS($v)" }

  /** Returns the remainder of one number divided into another. */
  def mod[V: Numeric](x: Sql[V]): Sql[T] = Sql { implicit ctx => sql"MOD($v, $x)" }

  /** Rounds a noninteger value upwards to the next greatest integer. Returns an integer value unchanged. */
  def ceil: Sql[T] = Sql { implicit ctx => sql"CEIL($v)" }

  /** Rounds a noninteger value downwards to the next least integer. Returns an integer value unchanged. */
  def floor: Sql[T] = Sql { implicit ctx => sql"FLOOR($v)" }

  /** Raises a value to the power of the mathematical constant known as e. */
  def exp(x: Sql[T]): Sql[T] = Sql { implicit ctx => sql"EXP($v, $x)" }

  /** Returns the natural logarithm of a number. */
  def ln: Sql[T] = Sql { implicit ctx => sql"LN($v)" }

  /** Raises a number to a specified power. */
  def pow(x: Sql[T]): Sql[T] = Sql { implicit ctx => sql"POW($v, $x)" }

  /** Computes the square root of a number. */
  def sqrt: Sql[T] = Sql { implicit ctx => sql"SQRT($v)" }
}
