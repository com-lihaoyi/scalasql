package scalasql.query

import scalasql._
import sourcecode.Text
import utest._
import utils.ScalaSqlSuite

import java.time.LocalDate

trait WithCteTests extends ScalaSqlSuite {
  def description = "Basic `WITH`/Common-Table-Expression operations"

  def tests = Tests {

    test("simple") - checker(
      query = Text {
        withCte(Buyer.select.map(_.name)) { x =>
          x.map(_ + "xxx")
        }
      },
      sqls = Seq(
        """
          WITH cte0 (res) AS (SELECT buyer0.name AS res FROM buyer buyer0)
          SELECT (cte0.res || ?) AS res
          FROM cte0
        """,
        """
          WITH cte0 (res) AS (SELECT buyer0.name AS res FROM buyer buyer0)
          SELECT CONCAT(cte0.res, ?) AS res
          FROM cte0
        """
      ),
      value = Seq("James Bondxxx", "叉烧包xxx", "Li Haoyixxx"),
      docs = """
        ScalaSql supports `WITH`-clauses, also known as "Common Table Expressions"
        (CTEs), via the `.withCte` syntax.
      """
    )

    test("eliminated") - checker(
      query = Text {
        withCte(Buyer.select) { x =>
          x.map(_.name + "xxx")
        }
      },
      sqls = Seq(
        """
          WITH cte0 (res__name) AS (SELECT buyer0.name AS res__name FROM buyer buyer0)
          SELECT (cte0.res__name || ?) AS res
          FROM cte0
        """,
        """
          WITH cte0 (res__name) AS (SELECT buyer0.name AS res__name FROM buyer buyer0)
          SELECT CONCAT(cte0.res__name, ?) AS res
          FROM cte0
        """
      ),
      value = Seq("James Bondxxx", "叉烧包xxx", "Li Haoyixxx"),
      docs = """
        Only the necessary columns are exported from the `WITH` clause; columns that
        are un-used in the downstream `SELECT` clause are eliminated
      """
    )
  }
}
