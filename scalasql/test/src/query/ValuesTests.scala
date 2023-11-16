package scalasql.query

import scalasql._
import sourcecode.Text
import utest._
import utils.ScalaSqlSuite

trait ValuesTests extends ScalaSqlSuite {
  def description = "Basic `SELECT`` operations: map, filter, join, etc."

  def tests = Tests {
    test("contains") - checker(
      query = Text { Values(Seq(1, 2, 3)).contains(1) },
      sql = "SELECT (? in (VALUES (?), (?), (?))) as res",
      value = true,
      docs = """
        You can use `Values` to generate a SQL `VALUES` clause
      """
    )
    test("max") - checker(
      query = Text { Values(Seq(1, 2, 3)).max },
      sql = "SELECT MAX(column1) AS res FROM (VALUES (?), (?), (?))",
      value = 3,
      docs = """
        `Values` supports aggregate functions like `.max`
      """
    )


  }
}
