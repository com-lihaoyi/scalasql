package scalasql.dialects

import scalasql.core.Sql
import scalasql.core.SqlStr.SqlStringSyntax

trait TrimOps {
  protected def v: Sql[String]

  /**
   * Trim [[x]]s from the left hand side of the string [[v]]
   */
  def ltrim(x: Sql[String]): Sql[String] = Sql { implicit ctx => sql"LTRIM($v, $x)" }

  /**
   * Trim [[x]]s from the right hand side of the string [[v]]
   */
  def rtrim(x: Sql[String]): Sql[String] = Sql { implicit ctx => sql"RTRIM($v, $x)" }
}
