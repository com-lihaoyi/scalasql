package scalasql.operations
import scalasql.core.Expr
import scalasql.core.SqlStr.SqlStringSyntax

trait PadOps {
  protected def v: Expr[?]

  def rpad(length: Expr[Int], fill: Expr[String]): Expr[String] = Expr { implicit ctx =>
    sql"RPAD($v, $length, $fill)"
  }

  def lpad(length: Expr[Int], fill: Expr[String]): Expr[String] = Expr { implicit ctx =>
    sql"LPAD($v, $length, $fill)"
  }
}
