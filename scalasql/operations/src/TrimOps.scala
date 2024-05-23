package scalasql.operations
import scalasql.core.Expr
import scalasql.core.SqlStr.SqlStringSyntax

trait TrimOps {
  protected def v: Expr[?]

  /**
   * Trim [[x]]s from the left hand side of the string [[v]]
   */
  def ltrim(x: Expr[String]): Expr[String] = Expr { implicit ctx => sql"LTRIM($v, $x)" }

  /**
   * Trim [[x]]s from the right hand side of the string [[v]]
   */
  def rtrim(x: Expr[String]): Expr[String] = Expr { implicit ctx => sql"RTRIM($v, $x)" }
}
