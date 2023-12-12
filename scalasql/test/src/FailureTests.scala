package scalasql
import scalasql.core.Expr
import utest._
import utils.SqliteSuite

import scala.annotation.unused

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
      assert(Expr.identity(Expr(1)) != Expr.identity(Expr(1)))
      val e = Expr(1)
      assert(Expr.identity(e) == Expr.identity(e))
    }
    test("toString") - {
      val ex = intercept[Exception] { Expr(1).toString }
      assert(ex.getMessage.contains("Expr#toString is not defined"))

      @unused val s: String = Expr.toString(Expr(1))
    }

  }
}
