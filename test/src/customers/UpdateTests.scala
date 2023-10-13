package usql.customers

import usql._
import utest._
import ExprOps._
/**
 * Tests for basic update operations: map, filter, join, etc.
 */
object UpdateTests extends TestSuite {
  val checker = new TestDb("querytests")
  def tests = Tests {
    test("simple") - {
      checker.db.run(
        Customer.update
          .filter(_.name === "John Doe")
          .set(_.birthdate -> "1960-10-30")
          .returning(_.id)

      )
    }
  }
}

