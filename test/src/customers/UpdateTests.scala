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
      checker(
        Customer.update
          .filter(_.name === "John Doe")
          .set(_.birthdate -> "2019-04-07")
          .returning(_.id)
      ).expect(
        sql = "UPDATE customer SET birthdate = ? WHERE customer.name = ? RETURNING customer.id as res",
        value = Vector(1)
      )

      checker(Customer.query.filter(_.name === "John Doe").map(_.birthdate)).expect(
        value = Vector("2019-04-07")
      )
    }

    test("multiple") - {
      checker(
        Customer.update
          .filter(_.name === "John Doe")
          .set(_.birthdate -> "2019-04-07", _.name -> "John Dee")
          .returning(_.id)
      ).expect(
        sql = "UPDATE customer SET birthdate = ?, name = ? WHERE customer.name = ? RETURNING customer.id as res",
        value = Vector(1)
      )

      checker(Customer.query.filter(_.name === "John Doe").map(_.birthdate)).expect(
        value = Nil
      )

      checker(Customer.query.filter(_.name === "John Dee").map(_.birthdate)).expect(
        value = Vector("2019-04-07")
      )
    }

    test("dynamic") - {
      checker(
        Customer.update
          .filter(_.name === "John Doe")
          .set(c => c.name -> c.name.toUpperCase)
          .returning(_.id)
      ).expect(
        sql = "UPDATE customer SET name = UPPER(customer.name) WHERE customer.name = ? RETURNING customer.id as res",
        value = Vector(1)
      )

      checker(Customer.query.filter(_.name === "John Doe").map(_.birthdate)).expect(
        value = Nil
      )

      checker(Customer.query.filter(_.name === "JOHN DOE").map(_.birthdate)).expect(
        value =  Vector("1960-10-30")
      )
    }

    test("join") - {
      checker(
        Customer.update
          .filter(_.name === "John Doe")
          .joinOn(PurchaseOrder.query)(_.id === _.customerId)
          .set(c => c._1.birthdate -> c._2.orderDate)
          .returning(_._1.id)
      ).expect(
        sql = """
          UPDATE customer
          SET birthdate = purchase_order0.order_date
          FROM purchase_order purchase_order0
          WHERE customer.id = purchase_order0.customer_id AND customer.name = ?
          RETURNING customer.id as res
        """,
        value = Vector(1)
      )

      checker(Customer.query.filter(_.name === "John Doe").map(_.birthdate)).expect(
        value = Vector("2018-02-13")
      )
    }

    test("multijoin") - {
      checker(
        Customer.update
          .filter(_.name === "John Doe")
          .joinOn(PurchaseOrder.query)(_.id === _.customerId)
          .joinOn(Item.query)(_._2.id === _.orderId)
          .joinOn(Product.query)(_._2.productId === _.id)
          .set(c => c._1._1._1.name -> c._2.name)
          .returning(_._1._1._1.id)
      ).expect(
        sql = """
          UPDATE customer
          SET name = product2.name
          FROM purchase_order purchase_order0
          JOIN item item1 ON purchase_order0.id = item1.order_id
          JOIN product product2 ON item1.product_id = product2.id
          WHERE customer.id = purchase_order0.customer_id AND customer.name = ?
          RETURNING customer.id as res
        """,
        value = Vector(1)
      )

      checker(Customer.query.filter(_.id === 1).map(_.name)).expect(
        value = Vector("Cell Phone")
      )
    }
  }
}

