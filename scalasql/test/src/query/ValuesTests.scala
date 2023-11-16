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
        "SELECT (? IN (VALUES (?), (?), (?))) AS res",
        "SELECT (? IN (VALUES ROW(?), ROW(?), ROW(?))) AS res"
      ),
      value = true,
      docs = """
        `Values` supports `.contains`
      """
    )

    test("max") - checker(
      query = Text { values(Seq(1, 2, 3)).max },
      sqls = Seq(
        "SELECT MAX(column1) AS res FROM (VALUES (?), (?), (?)) subquery0",
        "SELECT MAX(c1) AS res FROM (VALUES (?), (?), (?)) subquery0",
        "SELECT MAX(column_0) AS res FROM (VALUES ROW(?), ROW(?), ROW(?)) subquery0"
      ),
      value = 3,
      docs = """
        `Values` supports aggregate functions like `.max`
      """
    )

    test("map") - checker(
      query = Text { values(Seq(1, 2, 3)).map(_ + 1) },
      sqls = Seq(
        "SELECT (column1 + ?) AS res FROM (VALUES (?), (?), (?)) subquery0",
        "SELECT (c1 + ?) AS res FROM (VALUES (?), (?), (?)) subquery0",
        "SELECT (column_0 + ?) AS res FROM (VALUES ROW(?), ROW(?), ROW(?)) subquery0"
      ),
      value = Seq(2, 3, 4),
      docs = """
        `Values` supports most `.select` operators like `.map`, `filter`, and so on
      """
    )

    test("filter") - checker(
      query = Text { values(Seq(1, 2, 3)).filter(_ > 2) },
      sqls = Seq(
        "SELECT column1 AS res FROM (VALUES (?), (?), (?)) subquery0 WHERE (column1 > ?)",
        "SELECT c1 AS res FROM (VALUES (?), (?), (?)) subquery0 WHERE (c1 > ?)",
        "SELECT column_0 AS res FROM (VALUES ROW(?), ROW(?), ROW(?)) subquery0 WHERE (column_0 > ?)",
      ),
      value = Seq(3),
      docs = """
        `Values` supports most `.select` operators
      """
    )


  }
}
