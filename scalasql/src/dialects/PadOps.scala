package scalasql.dialects

import scalasql.core.Sql
import scalasql.core.SqlStr.SqlStringSyntax

trait PadOps {
  protected def v: Sql[String]

  def rpad(length: Sql[Int], fill: Sql[String]): Sql[String] = Sql { implicit ctx =>
    sql"RPAD($v, $length, $fill)"
  }

  def lpad(length: Sql[Int], fill: Sql[String]): Sql[String] = Sql { implicit ctx =>
    sql"LPAD($v, $length, $fill)"
  }
}
