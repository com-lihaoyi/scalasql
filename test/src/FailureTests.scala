package scalasql
import scalasql.query.Expr
import utest._
import utils.SqliteSuite

/**
 * Tests for all the aggregate operators that we provide by default
 */
object FailureTests extends TestSuite with SqliteSuite {

  def tests = Tests {
    test("equals") - {
      val ex = intercept[Exception] { Expr(1) == 2 }
      assert(ex.getMessage.contains("Expr#equals is not defined"))

      assert(Expr.getIdentity(Expr(1)) != Expr.getIdentity(Expr(1)))
      val e = Expr(1)
      assert(Expr.getIdentity(e) == Expr.getIdentity(e))
    }
    test("toString") - {
      val ex = intercept[Exception] { Expr(1).toString }
      assert(ex.getMessage.contains("Expr#toString is not defined"))

      val s: String = Expr.getToString(Expr(1))
    }

  }
}
