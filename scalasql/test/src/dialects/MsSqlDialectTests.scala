package scalasql.dialects

import scalasql._
import sourcecode.Text
import utest._
import utils.MsSqlSuite

trait MsSqlDialectTests extends MsSqlSuite {
  def description = "Operations specific to working with Microsoft SQL Databases"

  def tests = Tests {

    test("top") - checker(
      query = Buyer.select.take(0),
      sql = """
        SELECT TOP(?) buyer0.id AS id, buyer0.name AS name, buyer0.date_of_birth AS date_of_birth
        FROM buyer buyer0
      """,
      value = Seq[Buyer[Sc]](),
      docs = """
        For ScalaSql's Microsoft SQL dialect provides, the `.take(n)` operator translates
        into a SQL `TOP(n)` clause
      """
    )
  }
}
