package scalasql.query

import scalasql._
import sourcecode.Text
import utest._
import utils.ScalaSqlSuite

trait ValuesTests extends ScalaSqlSuite {
  def description = "Basic `SELECT`` operations: map, filter, join, etc."

  def tests = Tests {
    test("basic") - checker(
      query = Text { values(Seq(1, 2, 3)) },
      sqls = Seq("VALUES (?), (?), (?)", "VALUES ROW(?), ROW(?), ROW(?)"),
      value = Seq(1, 2, 3),
      docs = """
        You can use `Values` to generate a SQL `VALUES` clause
      """
    )

    test("contains") - checker(
      query = Text { values(Seq(1, 2, 3)).contains(1) },
      sqls = Seq(
        "SELECT (? in (VALUES (?), (?), (?))) as res",
        "SELECT (? in (VALUES ROW(?), ROW(?), ROW(?))) as res"
      ),
      value = true,
      docs = """
        `Values` supports `.contains`
      """
    )

    test("max") - checker(
      query = Text { values(Seq(1, 2, 3)).max },
      sqls = Seq(
        "SELECT MAX(column1) AS res FROM (VALUES (?), (?), (?)) v",
        "SELECT MAX(c1) AS res FROM (VALUES (?), (?), (?)) v",
        "SELECT MAX(column_0) AS res FROM (VALUES ROW(?), ROW(?), ROW(?)) v"
      ),
      value = 3,
      docs = """
        `Values` supports aggregate functions like `.max`
      """
    )


  }
}
