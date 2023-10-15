package usql.customers

import usql._
import utest._
import ExprOps._
/**
 * Tests for basic update operations
 */
object UpdateTests extends TestSuite {
  def tests = Tests {
    val checker = new TestDb("querytests")
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

    test("dynamic") - {
      val returned = checker.db.run(
        Customer.update
          .filter(_.name === "John Doe")
          .set(_.birthdate -> "2019-04-07", c => c.name -> c.name.toUpperCase)
          .returning(_.id)
      )

      assert(returned == Vector(1))

      checker(Customer.query.filter(_.name === "John Doe").map(_.birthdate)).expect(
        value = Nil
      )

      checker(Customer.query.filter(_.name === "JOHN DOE").map(_.birthdate)).expect(
        value = Vector("2019-04-07")
      )
    }
  }
}

