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
    test("sum") - checker(Item.select.map(_.quantity).sum).expect(
      sql = "SELECT SUM(item0.quantity) as res FROM item item0",
      value = 61
    )

    test("min") - checker(Item.select.map(_.quantity).min).expect(
      sql = "SELECT MIN(item0.quantity) as res FROM item item0",
      value = 2
    )

    test("max") - checker(Item.select.map(_.quantity).max).expect(
      sql = "SELECT MAX(item0.quantity) as res FROM item item0",
      value = 18
    )

    test("avg") - checker(Item.select.map(_.quantity).avg).expect(
      sql = "SELECT AVG(item0.quantity) as res FROM item item0",
      value = 8
    )
  }
}

