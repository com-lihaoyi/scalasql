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
          .set(_.birthdate -> "2019-04-07")
          .returning(_.id)
      )

      assert(returned == Vector(1))

      checker(Customer.query.filter(_.name === "John Doe").map(_.birthdate)).expect(
        value = Vector("2019-04-07")
      )
    }

    test("multiple") - {
      val returned = checker.db.run(
        Customer.update
          .filter(_.name === "John Doe")
          .set(_.birthdate -> "2019-04-07", _.name -> "John Dee")
          .returning(_.id)
      )

      assert(returned == Vector(1))

      checker(Customer.query.filter(_.name === "John Doe").map(_.birthdate)).expect(
        value = Nil
      )

      checker(Customer.query.filter(_.name === "John Dee").map(_.birthdate)).expect(
        value = Vector("2019-04-07")
      )
    }
  }
}

