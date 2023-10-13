package usql.customers

import usql._
import utest._
import ExprOps._

/**
 * Tests for all the individual symbolic operators and functions that we provide by default
 */
object ExprOpsTests extends TestSuite {
  val checker = new TestDb("expropstests")
  def tests = Tests {
    test("plus") - checker(Expr(6) + Expr(2)).expect(
      sql = "SELECT ? + ? as res",
      value = 8
    )

    test("minus") - checker(Expr(6) - Expr(2)).expect(
      sql = "SELECT ? - ? as res",
      value = 4
    )

    test("times") - checker(Expr(6) * Expr(2)).expect(
      sql = "SELECT ? * ? as res",
      value = 12
    )
    test("divide") - checker(Expr(6) / Expr(2)).expect(
      sql = "SELECT ? / ? as res",
      value = 3
    )

    test("modulo") - checker(Expr(6) % Expr(2)).expect(
      sql = "SELECT MOD(?, ?) as res",
      value = 0
    )

    test("greaterThan") - checker(Expr(6) > Expr(2)).expect(
      sql = "SELECT ? > ? as res",
      value = true
    )

    test("lessThan") - checker(Expr(6) < Expr(2)).expect(
      sql = "SELECT ? < ? as res",
      value = false
    )

    test("greaterThanOrEquals") - checker(Expr(6) >= Expr(2)).expect(
      sql = "SELECT ? >= ? as res",
      value = true
    )
    test("lessThanOrEquals") - checker(Expr(6) <= Expr(2)).expect(
      sql = "SELECT ? <= ? as res",
      value = false
    )
    test("bitwiseAnd") - checker(Expr(6) & Expr(2)).expect(
      sql = "SELECT ? & ? as res",
      value = 2
    )
    test("bitwiseOr") - checker(Expr(6) | Expr(3)).expect(
      sql = "SELECT ? | ? as res",
      value = 7
    )

    test("between") - checker(Expr(4).between(Expr(2), Expr(6))).expect(
      sql = "SELECT ? BETWEEN ? AND ? as res",
      value = true
    )
    test("unaryPlus") - checker(+Expr(-4)).expect(
      sql = "SELECT +? as res",
      value = -4
    )
    test("unaryMinus") - checker(-Expr(-4)).expect(
      sql = "SELECT -? as res",
      value = 4
    )
    test("unaryTilde") - checker(~Expr(-4)).expect(
      sql = "SELECT ~? as res",
      value = 3
    )
    test("abs") - checker(Expr(-4).abs).expect(
      sql = "SELECT ABS(?) as res",
      value = 4
    )
    test("mod") - checker(Expr(8).mod(Expr(3))).expect(
      sql = "SELECT MOD(?, ?) as res",
      value = 2
    )
    test("ceil") - checker(Expr(4.3).ceil).expect(
      sql = "SELECT CEIL(?) as res",
      value = 5
    )
    test("floor") - checker(Expr(4.7).floor).expect(
      sql = "SELECT FLOOR(?) as res",
      value = 4
    )
    test("floor") - checker(Expr(4.7).floor).expect(
      sql = "SELECT FLOOR(?) as res",
      value = 4
    )
  }
}

