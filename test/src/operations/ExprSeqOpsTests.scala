package usql.operations

import usql._
import utest._
import ExprOps._

/**
 * Tests for all the aggregate operators that we provide by default
 */
object ExprSeqOpsTests extends TestSuite {
  val checker = new TestDb("expropstests")
  def tests = Tests {

    test("size") - checker(Purchase.select.size).expect(
      sql = "SELECT COUNT(1) as res FROM purchase purchase0",
      value = 7
    )

    test("sumBy") - checker(Purchase.select.sumBy(_.count)).expect(
      sql = "SELECT SUM(purchase0.count) as res FROM purchase purchase0",
      value = 140
    )

    test("minBy") - checker(Purchase.select.minBy(_.count)).expect(
      sql = "SELECT MIN(purchase0.count) as res FROM purchase purchase0",
      value = 3
    )

    test("maxBy") - checker(Purchase.select.maxBy(_.count)).expect(
      sql = "SELECT MAX(purchase0.count) as res FROM purchase purchase0",
      value = 100
    )

    test("avgBy") - checker(Purchase.select.avgBy(_.count)).expect(
      sql = "SELECT AVG(purchase0.count) as res FROM purchase purchase0",
      value = 20
    )

    // Not supported by Sqlite

//    test("any") - checker(Purchase.query.any(_.count > 5)).expect(
//      sql = "SELECT ANY(purchase0.count > ?) as res FROM purchase purchase0",
//      value = true
//    )
//
//    test("all") - checker(Purchase.query.all(_.count > 5)).expect(
//      sql = "SELECT ALL(purchase0.count > ?) as res FROM purchase purchase0",
//      value = false
//    )

  }
}

