package scalasql.operations
import scalasql.core.SqlStr.SqlStringSyntax
import scalasql.core.Expr

trait MathOps {

  /** Converts radians to degrees */
  def degrees[T: Numeric](x: Expr[T]): Expr[Double] = Expr { implicit ctx => sql"DEGREES($x)" }

  /** Converts degrees to radians */
  def radians[T: Numeric](x: Expr[T]): Expr[Double] = Expr { implicit ctx => sql"RADIANS($x)" }

  /** `x` raised to the power of `y` */
  def power[T: Numeric](x: Expr[T], y: Expr[T]): Expr[Double] = Expr { implicit ctx =>
    sql"POWER($x, $y)"
  }

  /** Raises a value to the power of the mathematical constant known as e. */
  def exp[T: Numeric](x: Expr[T]): Expr[Double] = Expr { implicit ctx => sql"EXP($x)" }

  /** Returns the natural logarithm of a number. */
  def ln[T: Numeric](v: Expr[T]): Expr[Double] = Expr { implicit ctx => sql"LN($v)" }

  /** Logarithm of x to base b */
  def log[T: Numeric](b: Expr[Int], x: Expr[T]): Expr[Double] = Expr { implicit ctx =>
    sql"LOG($b, $x)"
  }

  /** Base 10 logarithm */
  def log10[T: Numeric](x: Expr[T]): Expr[Double] = Expr { implicit ctx => sql"LOG10($x)" }

  /** Computes the square root of a number. */
  def sqrt[T: Numeric](v: Expr[T]): Expr[Double] = Expr { implicit ctx => sql"SQRT($v)" }

  /** Calculate the trigonometric sine */
  def sin[T: Numeric](v: Expr[T]): Expr[Double] = Expr { implicit ctx => sql"SIN($v)" }

  /** Calculate the trigonometric cosine */
  def cos[T: Numeric](v: Expr[T]): Expr[Double] = Expr { implicit ctx => sql"COS($v)" }

  /** Calculate the trigonometric tangent */
  def tan[T: Numeric](v: Expr[T]): Expr[Double] = Expr { implicit ctx => sql"TAN($v)" }

  /** Calculate the arc sine */
  def asin[T: Numeric](v: Expr[T]): Expr[Double] = Expr { implicit ctx => sql"ASIN($v)" }

  /** Calculate the arc cosine */
  def acos[T: Numeric](v: Expr[T]): Expr[Double] = Expr { implicit ctx => sql"ACOS($v)" }

  /** Calculate the arc tangent */
  def atan[T: Numeric](v: Expr[T]): Expr[Double] = Expr { implicit ctx => sql"ATAN($v)" }

  /** Calculate the arc tangent */
  def atan2[T: Numeric](v: Expr[T], y: Expr[T]): Expr[Double] = Expr { implicit ctx =>
    sql"ATAN2($v, $y)"
  }

  /** Returns the value of Pi */
  def pi: Expr[Double] = Expr { _ => sql"PI()" }

}
