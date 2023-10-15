package usql.customers

import usql.ExprOps._
import usql._
import usql.query.Expr
import utest._

/**
 * Tests for all the individual symbolic operators and functions that we provide by default
 */
object ExprStringOpsTests extends TestSuite {
  val checker = new TestDb("strxpropstests")
  def tests = Tests {
    test("like") - checker(Expr("hello").like("he%")).expect(
      sql = "SELECT ? LIKE ? as res",
      value = true
    )

//    test("position") - checker(Expr("hello").position("ll")).expect(
//      sql = "SELECT POSITION(?, ?) as res",
//      value = 3
//    )

    test("toLowerCase") - checker(Expr("Hello").toLowerCase).expect(
      sql = "SELECT LOWER(?) as res",
      value = "hello"
    )

    test("trim") - checker(Expr("  Hello ").trim).expect(
      sql = "SELECT TRIM(?) as res",
      value = "Hello"
    )

    test("substring") - checker(Expr("Hello").substring(2, 2)).expect(
      sql = "SELECT SUBSTRING(?, ?, ?) as res",
      value = "el"
    )

//    test("overlay") - checker(Expr("Hello").overlay("LL", 2, 2)).expect(
//      sql = "SELECT OVERLAY(? PLACING ? FROM ? FOR ?) as res",
//      value = "HeLLo"
//    )
  }
}

