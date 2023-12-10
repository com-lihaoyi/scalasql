package scalasql.operations
import scalasql.core.Db
import scalasql.core.SqlStr.SqlStringSyntax

trait HyperbolicMathOps {

  /** Calculate the hyperbolic sine */
  def sinh[T: Numeric](v: Db[T]): Db[T] = Db { implicit ctx => sql"SINH($v)" }

  /** Calculate the hyperbolic cosine */
  def cosh[T: Numeric](v: Db[T]): Db[T] = Db { implicit ctx => sql"COSH($v)" }

  /** Calculate the hyperbolic tangent */
  def tanh[T: Numeric](v: Db[T]): Db[T] = Db { implicit ctx => sql"TANH($v)" }
}
