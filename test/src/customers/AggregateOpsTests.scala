package usql.customers

import usql.ExprOps._
import usql._
import utest._

/**
 * Tests for basic query operations: map, filter, join, etc.
 */
object AggregateOpsTests extends TestSuite {
  val checker = new TestDb("expropstests")
  def tests = Tests {
    test("sum") - checker(Item.query.map(_.quantity).sum).expect(
      sql = "SELECT SUM(item0.quantity) as res FROM item item0",
      value = 61
    )

    test("min") - checker(Item.query.map(_.quantity).min).expect(
      sql = "SELECT MIN(item0.quantity) as res FROM item item0",
      value = 2
    )

    test("max") - checker(Item.query.map(_.quantity).max).expect(
      sql = "SELECT MAX(item0.quantity) as res FROM item item0",
      value = 18
    )

    test("avg") - checker(Item.query.map(_.quantity).avg).expect(
      sql = "SELECT AVG(item0.quantity) as res FROM item item0",
      value = 8
    )

    test("size") - checker(Item.query.size).expect(
      sql = "SELECT COUNT(1) as res FROM item item0",
      value = 7
    )

    test("sumBy") - checker(Item.query.sumBy(_.quantity)).expect(
      sql = "SELECT SUM(item0.quantity) as res FROM item item0",
      value = 61
    )

    test("minBy") - checker(Item.query.minBy(_.quantity)).expect(
      sql = "SELECT MIN(item0.quantity) as res FROM item item0",
      value = 2
    )

    test("maxBy") - checker(Item.query.maxBy(_.quantity)).expect(
      sql = "SELECT MAX(item0.quantity) as res FROM item item0",
      value = 18
    )

    test("avgBy") - checker(Item.query.avgBy(_.quantity)).expect(
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

