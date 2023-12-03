package scalasql
import scalasql.core.Sql
import utest._
import utils.SqliteSuite

/**
 * Tests for all the aggregate operators that we provide by default
 */
object FailureTests extends SqliteSuite {
  def description = "Things that should not compile or should give runtime errors"
  def tests = Tests {
    test("equals") - {
//      val ex = intercept[Exception] { Sql(1) == 2 }
//      assert(ex.getMessage.contains("Sql#equals is not defined"))
//
      assert(Sql.identity(Sql(1)) != Sql.identity(Sql(1)))
      val e = Sql(1)
      assert(Sql.identity(e) == Sql.identity(e))
    }
    test("toString") - {
      val ex = intercept[Exception] { Sql(1).toString }
      assert(ex.getMessage.contains("Sql#toString is not defined"))

      val s: String = Sql.toString(Sql(1))
    }

  }
}
