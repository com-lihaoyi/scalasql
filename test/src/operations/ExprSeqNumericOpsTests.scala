package usql.operations

import usql._
import utest._
import ExprOps._

/**
 * Tests for all the aggregate operators that we provide by default
 */
object ExprSeqNumericOpsTests extends TestSuite {
  val checker = new TestDb("expropstests")
  def tests = Tests {
    test("sum") - checker(Purchase.select.map(_.count).sum).expect(
      sql = "SELECT SUM(purchase0.count) as res FROM purchase purchase0",
      value = 140
    )

    test("min") - checker(Purchase.select.map(_.count).min).expect(
      sql = "SELECT MIN(purchase0.count) as res FROM purchase purchase0",
      value = 3
    )

    test("max") - checker(Purchase.select.map(_.count).max).expect(
      sql = "SELECT MAX(purchase0.count) as res FROM purchase purchase0",
      value = 100
    )

    test("avg") - checker(Purchase.select.map(_.count).avg).expect(
      sql = "SELECT AVG(purchase0.count) as res FROM purchase purchase0",
      value = 20
    )
  }
}

