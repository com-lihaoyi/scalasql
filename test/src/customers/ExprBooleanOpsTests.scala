package customers

import usql.ExprOps._
import usql._
import usql.query.Expr
import utest._

/**
 * Tests for all the individual symbolic operators and functions that we provide by default
 */
object ExprBooleanOpsTests extends TestSuite {
  val checker = new TestDb("eexpropstests")
  def tests = Tests {
    test("and") - checker(Expr(true) && Expr(true)).expect(
      sql = "SELECT ? AND ? as res",
      value = true
    )

    test("or") - checker(Expr(false) || Expr(false)).expect(
      sql = "SELECT ? OR ? as res",
      value = false
    )

    test("or") - checker(!Expr(false)).expect(
      sql = "SELECT NOT ? as res",
      value = true
    )
  }
}

