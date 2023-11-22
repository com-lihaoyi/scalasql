package scalasql
import scalasql.query.Expr
import utest._
import utils.SqliteSuite

/**
 * Tests for all the aggregate operators that we provide by default
 */
object FailureTests extends SqliteSuite {
  def description = "Things that should not compile or should give runtime errors"
  def tests = Tests {
    test("equals") - {
//      val ex = intercept[Exception] { Expr(1) == 2 }
//      assert(ex.getMessage.contains("Expr#equals is not defined"))
//
      assert(Expr.exprIdentity(Expr(1)) != Expr.exprIdentity(Expr(1)))
      val e = Expr(1)
      assert(Expr.exprIdentity(e) == Expr.exprIdentity(e))
    }
    test("toString") - {
      val ex = intercept[Exception] { Expr(1).toString }
      assert(ex.getMessage.contains("Expr#toString is not defined"))

      val s: String = Expr.exprToString(Expr(1))
    }

  }
}
