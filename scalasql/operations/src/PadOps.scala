package scalasql.operations
import scalasql.core.Db
import scalasql.core.SqlStr.SqlStringSyntax

trait PadOps {
  protected def v: Db[_]

  def rpad(length: Db[Int], fill: Db[String]): Db[String] = Db { implicit ctx =>
    sql"RPAD($v, $length, $fill)"
  }

  def lpad(length: Db[Int], fill: Db[String]): Db[String] = Db { implicit ctx =>
    sql"LPAD($v, $length, $fill)"
  }
}
