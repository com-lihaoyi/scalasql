package scalasql.dialects

import scalasql.query.Expr
import scalasql.renderer.SqlStr.SqlStringSyntax

trait PadOps {
  protected def v: Expr[String]

  def rpad(length: Expr[Int], fill: Expr[String]): Expr[String] = Expr { implicit ctx =>
    sql"RPAD($v, $length, $fill)"
  }

  def lpad(length: Expr[Int], fill: Expr[String]): Expr[String] = Expr { implicit ctx =>
    sql"LPAD($v, $length, $fill)"
  }
}
