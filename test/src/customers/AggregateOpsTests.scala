package usql.customers

import usql.ExprOps._
import usql._
import utest._

/**
 * Tests for all the aggregate operators that we provide by default
 */
object AggregateOpsTests extends TestSuite {
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

    test("size") - checker(Item.select.size).expect(
      sql = "SELECT COUNT(1) as res FROM item item0",
      value = 7
    )

    test("sumBy") - checker(Item.select.sumBy(_.quantity)).expect(
      sql = "SELECT SUM(item0.quantity) as res FROM item item0",
      value = 61
    )

    test("minBy") - checker(Item.select.minBy(_.quantity)).expect(
      sql = "SELECT MIN(item0.quantity) as res FROM item item0",
      value = 2
    )

    test("maxBy") - checker(Item.select.maxBy(_.quantity)).expect(
      sql = "SELECT MAX(item0.quantity) as res FROM item item0",
      value = 18
    )

    test("avgBy") - checker(Item.select.avgBy(_.quantity)).expect(
      sql = "SELECT AVG(item0.quantity) as res FROM item item0",
      value = 8
    )

    // Not supported by Sqlite

//    test("any") - checker(Item.query.any(_.quantity > 5)).expect(
//      sql = "SELECT ANY(item0.quantity > ?) as res FROM item item0",
//      value = true
//    )
//
//    test("all") - checker(Item.query.all(_.quantity > 5)).expect(
//      sql = "SELECT ALL(item0.quantity > ?) as res FROM item item0",
//      value = false
//    )

  }
}

