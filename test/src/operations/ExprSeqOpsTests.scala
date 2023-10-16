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
    test("contains") - checker(
      Buyer.select.filter(b => ShippingInfo.select.map(_.buyerId).contains(b.id))
    ).expect(
      sql = """
        SELECT buyer0.id as res__id, buyer0.name as res__name, buyer0.birthdate as res__birthdate
        FROM buyer buyer0
        WHERE buyer0.id in (SELECT shipping_info0.buyer_id as res FROM shipping_info shipping_info0)
      """,
      value = Vector(
        Buyer(1, "James Bond", "2001-02-03"),
        Buyer(2, "叉烧包", "1923-11-12")
      )
    )

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

