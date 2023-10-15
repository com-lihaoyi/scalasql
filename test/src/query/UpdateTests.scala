package usql.query

import usql._
import utest._
import ExprOps._

/**
 * Tests for basic update operations
 */
object UpdateTests extends TestSuite {
  def tests = Tests {
    val checker = new TestDb("querytests")
    test("update") - {
      checker(Customer.update.filter(_.name === "James Bond").set(_.birthdate -> "2019-04-07"))
        .expect(
          sql = "UPDATE customer SET birthdate = ? WHERE customer.name = ?",
          value = 1
        )

      checker(Customer.select.filter(_.name === "James Bond").map(_.birthdate)).expect(
        value = Vector("2019-04-07")
      )

      checker(Customer.select.filter(_.name === "Li Haoyi").map(_.birthdate)).expect(
        value = Vector("1965-08-09") // not updated
      )
    }

    test("bulk") - {
      checker(Customer.update.set(_.birthdate -> "2019-04-07")).expect(
        sql = "UPDATE customer SET birthdate = ?",
        value = 3
      )

      checker(Customer.select.filter(_.name === "James Bond").map(_.birthdate)).expect(
        value = Vector("2019-04-07")
      )
      checker(Customer.select.filter(_.name === "Li Haoyi").map(_.birthdate)).expect(
        value = Vector("2019-04-07")
      )
    }

    test("returning") - {
      checker(
        Customer.update
          .filter(_.name === "James Bond")
          .set(_.birthdate -> "2019-04-07")
          .returning(_.id)
      ).expect(
        sql = "UPDATE customer SET birthdate = ? WHERE customer.name = ? RETURNING customer.id as res",
        value = Vector(1)
      )

      checker(Customer.select.filter(_.name === "James Bond").map(_.birthdate)).expect(
        value = Vector("2019-04-07")
      )
    }

    test("multiple") - {
      checker(
        Customer.update
          .filter(_.name === "James Bond")
          .set(_.birthdate -> "2019-04-07", _.name -> "John Dee")
          .returning(_.id)
      ).expect(
        sql = "UPDATE customer SET birthdate = ?, name = ? WHERE customer.name = ? RETURNING customer.id as res",
        value = Vector(1)
      )

      checker(Customer.select.filter(_.name === "James Bond").map(_.birthdate)).expect(
        value = Nil
      )

      checker(Customer.select.filter(_.name === "John Dee").map(_.birthdate)).expect(
        value = Vector("2019-04-07")
      )
    }

    test("dynamic") - {
      checker(
        Customer.update
          .filter(_.name === "James Bond")
          .set(c => c.name -> c.name.toUpperCase)
          .returning(_.id)
      ).expect(
        sql = "UPDATE customer SET name = UPPER(customer.name) WHERE customer.name = ? RETURNING customer.id as res",
        value = Vector(1)
      )

      checker(Customer.select.filter(_.name === "James Bond").map(_.birthdate)).expect(
        value = Nil
      )

      checker(Customer.select.filter(_.name === "JAMES BOND").map(_.birthdate)).expect(
        value =  Vector("2001-02-03")
      )
    }

    test("join") - {
      checker(
        Customer.update
          .filter(_.name === "James Bond")
          .joinOn(PurchaseOrder.select)(_.id === _.customerId)
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

      checker(Customer.select.filter(_.name === "James Bond").map(_.birthdate)).expect(
        value = Vector("2012-04-05")
      )
    }

    test("multijoin") - {
      checker(
        Customer.update
          .filter(_.name === "James Bond")
          .joinOn(PurchaseOrder)(_.id === _.customerId)
          .joinOn(Item)(_._2.id === _.orderId)
          .joinOn(Product)(_._2.productId === _.id)
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

      checker(Customer.select.filter(_.id === 1).map(_.name)).expect(
        value = Vector("Camera")
      )
    }
  }
}

