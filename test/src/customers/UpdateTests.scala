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
      val returned = checker.db.run(
        Customer.update
          .filter(_.name === "John Doe")
          .set(_.birthdate -> "1990-10-30")
          .returning(_.id)
      )

      assert(returned == Vector(1))

      checker(Customer.query.filter(_.name === "John Doe").map(_.birthdate)).expect(
        sql = "SELECT customer0.birthdate as res FROM customer customer0 WHERE customer0.name = ?",
        value = Vector("1990-10-30")
      )
    }
  }
}

