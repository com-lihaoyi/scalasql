package scalasql.operations

import scalasql.TypeMapper
import scalasql.query.Expr
import scalasql.renderer.SqlStr.SqlStringSyntax

class ExprNumericOps[T: Numeric](v: Expr[T])(implicit val m: TypeMapper[T]) {

  /** Addition */
  def +[V: Numeric](x: Expr[V]): Expr[T] = Expr { implicit ctx => sql"$v + $x" }

  /** Subtraction */
  def -[V: Numeric](x: Expr[V]): Expr[T] = Expr { implicit ctx => sql"$v - $x" }

  /** Multiplication */
  def *[V: Numeric](x: Expr[V]): Expr[T] = Expr { implicit ctx => sql"$v * $x" }

  /** Division */
  def /[V: Numeric](x: Expr[V]): Expr[T] = Expr { implicit ctx => sql"$v / $x" }

  /** Remainder */
  def %[V: Numeric](x: Expr[V]): Expr[T] = Expr { implicit ctx => sql"MOD($v, $x)" }

  /** Bitwise AND */
  def &[V: Numeric](x: Expr[V]): Expr[T] = Expr { implicit ctx => sql"$v & $x" }

  /** Bitwise OR */
  def |[V: Numeric](x: Expr[V]): Expr[T] = Expr { implicit ctx => sql"$v | $x" }

  /** Bitwise XOR */
  def ^[V: Numeric](x: Expr[V]): Expr[T] = Expr { implicit ctx => sql"$v ^ $x" }

  /** TRUE if the operand is within a range */
  def between(x: Expr[Int], y: Expr[Int]): Expr[Boolean] = Expr { implicit ctx =>
    sql"$v BETWEEN $x AND $y"
  }

  /** Unary Positive Operator */
  def unary_+ : Expr[T] = Expr { implicit ctx => sql"+$v" }

  /** Unary Negation Operator */
  def unary_- : Expr[T] = Expr { implicit ctx => sql"-$v" }

  /** Unary Bitwise NOT Operator */
  def unary_~ : Expr[T] = Expr { implicit ctx => sql"~$v" }

  /** Returns the absolute value of a number. */
  def abs: Expr[T] = Expr { implicit ctx => sql"ABS($v)" }

  /** Returns the remainder of one number divided into another. */
  def mod[V: Numeric](x: Expr[V]): Expr[T] = Expr { implicit ctx => sql"MOD($v, $x)" }

  /** Rounds a noninteger value upwards to the next greatest integer. Returns an integer value unchanged. */
  def ceil: Expr[T] = Expr { implicit ctx => sql"CEIL($v)" }

  /** Rounds a noninteger value downwards to the next least integer. Returns an integer value unchanged. */
  def floor: Expr[T] = Expr { implicit ctx => sql"FLOOR($v)" }

  /** Raises a value to the power of the mathematical constant known as e. */
  def exp(x: Expr[T]): Expr[T] = Expr { implicit ctx => sql"EXP($v, $x)" }

  /** Returns the natural logarithm of a number. */
  def ln: Expr[T] = Expr { implicit ctx => sql"LN($v)" }

  /** Raises a number to a specified power. */
  def pow(x: Expr[T]): Expr[T] = Expr { implicit ctx => sql"POW($v, $x)" }

  /** Computes the square root of a number. */
  def sqrt: Expr[T] = Expr { implicit ctx => sql"SQRT($v)" }
}
