package scalasql
import scalasql.core.Db
import utest._
import utils.SqliteSuite

/**
 * Tests for all the aggregate operators that we provide by default
 */
object FailureTests extends SqliteSuite {
  def description = "Things that should not compile or should give runtime errors"
  def tests = Tests {
    test("equals") - {
//      val ex = intercept[Exception] { Db(1) == 2 }
//      assert(ex.getMessage.contains("Db#equals is not defined"))
//
      assert(Db.identity(Db(1)) != Db.identity(Db(1)))
      val e = Db(1)
      assert(Db.identity(e) == Db.identity(e))
    }
    test("toString") - {
      val ex = intercept[Exception] { Db(1).toString }
      assert(ex.getMessage.contains("Db#toString is not defined"))

      val s: String = Db.toString(Db(1))
    }

  }
}
