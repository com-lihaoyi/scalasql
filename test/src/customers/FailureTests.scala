package usql.customers

import usql._
import usql.query.Expr
import utest._

/**
 * Tests for all the aggregate operators that we provide by default
 */
object FailureTests extends TestSuite {
  val checker = new TestDb("expropstests")
  def tests = Tests {
    test("equals") - {
      val ex = intercept[Exception]{Expr(1) == 2}
      assert(ex.getMessage == "Expr#equals is not defined")
    }
    test("toString") - {
      val ex = intercept[Exception]{Expr(1).toString}
      assert(ex.getMessage == "Expr#toString is not defined")
    }

  }
}

