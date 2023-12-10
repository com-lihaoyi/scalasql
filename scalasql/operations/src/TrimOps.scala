package scalasql.operations
import scalasql.core.Db
import scalasql.core.SqlStr.SqlStringSyntax

trait TrimOps {
  protected def v: Db[_]

  /**
   * Trim [[x]]s from the left hand side of the string [[v]]
   */
  def ltrim(x: Db[String]): Db[String] = Db { implicit ctx => sql"LTRIM($v, $x)" }

  /**
   * Trim [[x]]s from the right hand side of the string [[v]]
   */
  def rtrim(x: Db[String]): Db[String] = Db { implicit ctx => sql"RTRIM($v, $x)" }
}
