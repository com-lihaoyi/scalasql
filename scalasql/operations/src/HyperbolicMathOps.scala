package scalasql.operations
import scalasql.core.Expr
import scalasql.core.SqlStr.SqlStringSyntax

trait HyperbolicMathOps {

  /** Calculate the hyperbolic sine */
  def sinh[T: Numeric](v: Expr[T]): Expr[T] = Expr { implicit ctx => sql"SINH($v)" }

  /** Calculate the hyperbolic cosine */
  def cosh[T: Numeric](v: Expr[T]): Expr[T] = Expr { implicit ctx => sql"COSH($v)" }

  /** Calculate the hyperbolic tangent */
  def tanh[T: Numeric](v: Expr[T]): Expr[T] = Expr { implicit ctx => sql"TANH($v)" }
}
