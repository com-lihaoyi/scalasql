package scalasql.buyers

import scalasql._
import scalasql.query.Expr
import utest._

/**
 * Tests for all the aggregate operators that we provide by default
 */
object FailureTests extends TestSuite with SqliteSuite {

  def tests = Tests {
    test("equals") - {
      val ex = intercept[Exception] { Expr(1) == 2 }
      assert(ex.getMessage.contains("Expr#equals is not defined"))

      assert(Expr(1).exprIdentity != Expr(1).exprIdentity)
      val e = Expr(1)
      assert(e.exprIdentity == e.exprIdentity)
    }
    test("toString") - {
      val ex = intercept[Exception] { Expr(1).toString }
      assert(ex.getMessage.contains("Expr#toString is not defined"))

      val s: String = Expr(1).exprToString
    }

  }
}
