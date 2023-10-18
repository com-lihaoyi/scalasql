package usql.operations

import usql.query.Expr
import usql.renderer.SqlStr.SqlStringSyntax

class ExprNumericOps[T: Numeric](v: Expr[T]) {

  /** Addition */
  def +[V: Numeric](x: Expr[V]): Expr[T] = Expr { implicit ctx => usql"$v + $x" }

  /** Subtraction */
  def -[V: Numeric](x: Expr[V]): Expr[T] = Expr { implicit ctx => usql"$v - $x" }

  /** Multiplication */
  def *[V: Numeric](x: Expr[V]): Expr[T] = Expr { implicit ctx => usql"$v * $x" }

  /** Division */
  def /[V: Numeric](x: Expr[V]): Expr[T] = Expr { implicit ctx => usql"$v / $x" }

  /** Remainder */
  def %[V: Numeric](x: Expr[V]): Expr[T] = Expr { implicit ctx => usql"MOD($v, $x)" }

  /** Greater than */
  def >[V: Numeric](x: Expr[V]): Expr[Boolean] = Expr { implicit ctx => usql"$v > $x" }

  /** Less than */
  def <[V: Numeric](x: Expr[V]): Expr[Boolean] = Expr { implicit ctx => usql"$v < $x" }

  /** Greater than or equal to */
  def >=[V: Numeric](x: Expr[V]): Expr[Boolean] = Expr { implicit ctx => usql"$v >= $x" }

  /** Less than or equal to */
  def <=[V: Numeric](x: Expr[V]): Expr[Boolean] = Expr { implicit ctx => usql"$v <= $x" }

  /** Bitwise AND */
  def &[V: Numeric](x: Expr[V]): Expr[T] = Expr { implicit ctx => usql"$v & $x" }

  /** Bitwise OR */
  def |[V: Numeric](x: Expr[V]): Expr[T] = Expr { implicit ctx => usql"$v | $x" }

  /** Bitwise XOR */
  def ^[V: Numeric](x: Expr[V]): Expr[T] = Expr { implicit ctx => usql"$v ^ $x" }

  /** TRUE if the operand is within a range */
  def between(x: Expr[Int], y: Expr[Int]): Expr[Boolean] = Expr { implicit ctx =>
    usql"$v BETWEEN $x AND $y"
  }

  /** Unary Positive Operator */
  def unary_+ : Expr[T] = Expr { implicit ctx => usql"+$v" }

  /** Unary Negation Operator */
  def unary_- : Expr[T] = Expr { implicit ctx => usql"-$v" }

  /** Unary Bitwise NOT Operator */
  def unary_~ : Expr[T] = Expr { implicit ctx => usql"~$v" }

  /** Returns the absolute value of a number. */
  def abs: Expr[T] = Expr { implicit ctx => usql"ABS($v)" }

  /** Returns the remainder of one number divided into another. */
  def mod[V: Numeric](x: Expr[V]): Expr[T] = Expr { implicit ctx => usql"MOD($v, $x)" }

  /** Rounds a noninteger value upwards to the next greatest integer. Returns an integer value unchanged. */
  def ceil: Expr[T] = Expr { implicit ctx => usql"CEIL($v)" }

  /** Rounds a noninteger value downwards to the next least integer. Returns an integer value unchanged. */
  def floor: Expr[T] = Expr { implicit ctx => usql"FLOOR($v)" }

  /** Raises a value to the power of the mathematical constant known as e. */
  def exp(x: Expr[T]): Expr[T] = Expr { implicit ctx => usql"EXP($v, $x)" }

  /** Returns the natural logarithm of a number. */
  def ln: Expr[T] = Expr { implicit ctx => usql"LN($v)" }

  /** Raises a number to a specified power. */
  def pow(x: Expr[T]): Expr[T] = Expr { implicit ctx => usql"POW($v, $x)" }

  /** Computes the square root of a number. */
  def sqrt: Expr[T] = Expr { implicit ctx => usql"SQRT($v)" }
}
