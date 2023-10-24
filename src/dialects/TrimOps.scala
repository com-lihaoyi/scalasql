package scalasql.dialects

import scalasql.query.Expr
import scalasql.renderer.SqlStr.SqlStringSyntax

trait TrimOps {
  def v: Expr[String]
  def ltrim(x: Expr[String]): Expr[String] = Expr { implicit ctx => sql"LTRIM($v, $x)" }
  def rtrim(x: Expr[String]): Expr[String] = Expr { implicit ctx => sql"RTRIM($v, $x)" }
}
