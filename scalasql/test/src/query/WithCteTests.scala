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
        withCte(Buyer.select.map(_.name)){x =>
          x.map(_ + "xxx")
        }
      },
      sqls = Seq(
        """
          WITH cte(res) AS (SELECT buyer0.name AS res FROM buyer buyer0)
          SELECT (res || ?) AS res
          FROM cte
        """,
        """
          WITH cte(res) AS (SELECT buyer0.name AS res FROM buyer buyer0)
          SELECT CONCAT(res, ?) AS res
          FROM cte
        """
      ),
      value = Seq("James Bondxxx", "叉烧包xxx", "Li Haoyixxx"),
      docs = """
        ScalaSql supports `WITH`-clauses, also known as "Common Table Expressions"
        (CTEs), via the `.withCte` syntax.
      """
    )
  }
}
