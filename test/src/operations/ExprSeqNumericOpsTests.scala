package usql.operations

import usql._
import utest._
import ExprOps._

object SqliteExprSeqNumericOpsTests extends ExprSeqNumericOpsTests with SqliteSuite
object PgExprExprSeqNumericOpsTests extends ExprSeqNumericOpsTests with PostgresSuite

/**
 * Tests for all the aggregate operators that we provide by default
 */
trait ExprSeqNumericOpsTests extends TestSuite {
  val checker: TestDb
  def tests = Tests {
    test("sum") - checker(
      query = Purchase.select.map(_.count).sum,
      sql = "SELECT SUM(purchase0.count) as res FROM purchase purchase0",
      value = 140
    )

    test("min") - checker(
      query = Purchase.select.map(_.count).min,
      sql = "SELECT MIN(purchase0.count) as res FROM purchase purchase0",
      value = 3
    )

    test("max") - checker(
      query = Purchase.select.map(_.count).max,
      sql = "SELECT MAX(purchase0.count) as res FROM purchase purchase0",
      value = 100
    )

    test("avg") - checker(
      query = Purchase.select.map(_.count).avg,
      sql = "SELECT AVG(purchase0.count) as res FROM purchase purchase0",
      value = 20
    )
  }
}
