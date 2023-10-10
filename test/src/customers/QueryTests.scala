package usql.customers

import utest._
import usql._
import ExprOps._

/**
 * Tests for basic query operations: map, filter, join, etc.
 */
object QueryTests extends TestSuite {
  val checker = new TestDb("querytests")
  def tests = Tests {
    test("constant") - checker(Expr(1)).expect(
      sql = "SELECT ? as res",
      value = 1
    )

    test("table") - checker(Customer.query).expect(
      sql = """
        SELECT
          customer0.id as res__id,
          customer0.name as res__name,
          customer0.birthdate as res__birthdate
        FROM customer customer0
      """,
      value = Vector(
        Customer(id = 1, name = "John Doe", birthdate = "1960-10-30"),
        Customer(id = 2, name = "Pepito Pérez", birthdate = "1954-07-15"),
        Customer(id = 3, name = "Cosme Fulanito", birthdate = "1956-05-12")
      )
    )

    test("filter"){
      test("single") - checker(PurchaseOrder.query.filter(_.customerId === 2)).expect(
        sql = """
        SELECT
          purchase_order0.id as res__id,
          purchase_order0.customer_id as res__customer_id,
          purchase_order0.order_date as res__order_date
        FROM purchase_order purchase_order0
        WHERE purchase_order0.customer_id = ?
      """,
        value = Vector(
          PurchaseOrder(id = 1, customerId = 2, orderDate = "2018-01-04"),
          PurchaseOrder(id = 3, customerId = 2, orderDate = "2018-02-25")
        )
      )

      test("multiple") - checker(
        PurchaseOrder.query.filter(_.customerId === 2).filter(_.orderDate === "2018-02-25")
      ).expect(
        sql = """
        SELECT
          purchase_order0.id as res__id,
          purchase_order0.customer_id as res__customer_id,
          purchase_order0.order_date as res__order_date
        FROM purchase_order purchase_order0
        WHERE purchase_order0.customer_id = ?
        AND purchase_order0.order_date = ?
      """,
        value = Vector(
          PurchaseOrder(id = 3, customerId = 2, orderDate = "2018-02-25")
        )
      )
      test("combined") - checker(
        PurchaseOrder.query.filter(p => p.customerId === 2 && p.orderDate === "2018-02-25")
      ).expect(
        sql = """
        SELECT
          purchase_order0.id as res__id,
          purchase_order0.customer_id as res__customer_id,
          purchase_order0.order_date as res__order_date
        FROM purchase_order purchase_order0
        WHERE purchase_order0.customer_id = ?
        AND purchase_order0.order_date = ?
      """,
        value = Vector(
          PurchaseOrder(id = 3, customerId = 2, orderDate = "2018-02-25")
        )
      )
    }

    test("map"){
      test("single") - checker(Customer.query.map(_.name)).expect(
        sql = "SELECT customer0.name as res FROM customer customer0",
        value = Vector("John Doe", "Pepito Pérez", "Cosme Fulanito")
      )

      test("tuple2") - checker(Customer.query.map(c => (c.name, c.id))).expect(
        sql = "SELECT customer0.name as res__0, customer0.id as res__1 FROM customer customer0",
        value =  Vector(("John Doe", 1), ("Pepito Pérez", 2), ("Cosme Fulanito", 3))
      )

      test("tuple3") - checker(Customer.query.map(c => (c.name, c.id, c.birthdate))).expect(
        sql = """
          SELECT
            customer0.name as res__0,
            customer0.id as res__1,
            customer0.birthdate as res__2
          FROM customer customer0
        """,
        value =  Vector(
          ("John Doe", 1, "1960-10-30"),
          ("Pepito Pérez", 2, "1954-07-15"),
          ("Cosme Fulanito", 3, "1956-05-12")
        )
      )

      test("interpolateInMap") - checker(Product.query.map(_.price * 2)).expect(
        sql = "SELECT product0.price * ? as res FROM product product0",
        value = Vector(15.98, 703.92, 7.14, 262.0, 2000.0, 2.0)
      )

      test("heterogenousTuple") - checker(Customer.query.map(c => (c.id, c))).expect(
        sql = """
          SELECT
            customer0.id as res__0,
            customer0.id as res__1__id,
            customer0.name as res__1__name,
            customer0.birthdate as res__1__birthdate
          FROM customer customer0
        """,
        value = Vector(
          (1, Customer(id = 1, name = "John Doe", birthdate = "1960-10-30")),
          (2, Customer(id = 2, name = "Pepito Pérez", birthdate = "1954-07-15")),
          (3, Customer(id = 3, name = "Cosme Fulanito", birthdate = "1956-05-12"))
        )
      )
    }

    test("filterMap") - checker(Product.query.filter(_.price < 100).map(_.name)).expect(
      sql = "SELECT product0.name as res FROM product product0 WHERE product0.price < ?",
      value = Vector("Keyboard", "Shirt", "Spoon")
    )

    test("aggregate") - checker(Item.query.sumBy(_.total)).expect(
      sql = "SELECT SUM(item0.total) as res FROM item item0",
      value = 16144.74
    )

    test("sort") {
      test("sort") - checker(Product.query.sortBy(_.price).map(_.name)).expect(
        sql = "SELECT product0.name as res FROM product product0 ORDER BY product0.price",
        value = Vector("Spoon", "Shirt", "Keyboard", "Bed", "Television", "Cell Phone")
      )

      test("sortLimit") - checker(Product.query.sortBy(_.price).map(_.name).take(2)).expect(
        sql = "SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 2",
        value = Vector("Spoon", "Shirt")
      )

      test("sortLimitTwiceHigher") - checker(Product.query.sortBy(_.price).map(_.name).take(2).take(3)).expect(
        sql = "SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 2",
        value = Vector("Spoon", "Shirt")
      )

      test("sortLimitTwiceLower") - checker(Product.query.sortBy(_.price).map(_.name).take(2).take(1)).expect(
        sql = "SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 1",
        value = Vector("Spoon")
      )

      test("sortOffset") - checker(Product.query.sortBy(_.price).map(_.name).drop(2)).expect(
        sql = "SELECT product0.name as res FROM product product0 ORDER BY product0.price OFFSET 2",
        value = Vector("Keyboard", "Bed", "Television", "Cell Phone")
      )

      test("sortOffsetTwice") - checker(Product.query.sortBy(_.price).map(_.name).drop(2).drop(2)).expect(
        sql = "SELECT product0.name as res FROM product product0 ORDER BY product0.price OFFSET 4",
        value = Vector("Television", "Cell Phone")
      )

      test("sortOffsetLimit") - checker(Product.query.sortBy(_.price).map(_.name).drop(2).take(2)).expect(
        sql = "SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 2 OFFSET 2",
        value = Vector("Keyboard", "Bed")
      )

      test("sortLimitOffset") - checker(Product.query.sortBy(_.price).map(_.name).take(2).drop(1)).expect(
        sql = "SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 1 OFFSET 1",
        value = Vector("Shirt")
      )
    }

    test("joins"){
      test("joinFilter") - checker(
        Customer.query.joinOn(PurchaseOrder.query)(_.id === _.customerId)
          .filter(_._1.name === "Pepito Pérez")
      ).expect(
        sql = """
          SELECT
            customer0.id as res__0__id,
            customer0.name as res__0__name,
            customer0.birthdate as res__0__birthdate,
            purchase_order1.id as res__1__id,
            purchase_order1.customer_id as res__1__customer_id,
            purchase_order1.order_date as res__1__order_date
          FROM customer customer0
          JOIN purchase_order purchase_order1 ON customer0.id = purchase_order1.customer_id
          WHERE customer0.name = ?
        """,
        value = Vector(
          (
            Customer(id = 2, name = "Pepito Pérez", birthdate = "1954-07-15"),
            PurchaseOrder(id = 1, customerId = 2, orderDate = "2018-01-04")
          ),
          (
            Customer(id = 2, name = "Pepito Pérez", birthdate = "1954-07-15"),
            PurchaseOrder(id = 3, customerId = 2, orderDate = "2018-02-25")
          )
        )
      )

      test("joinFilterMap") - checker(
        Customer.query.joinOn(PurchaseOrder.query)(_.id === _.customerId)
          .filter(_._1.name === "John Doe")
          .map(_._2.orderDate)
      ).expect(
        sql = """
          SELECT purchase_order1.order_date as res
          FROM customer customer0
          JOIN purchase_order purchase_order1 ON customer0.id = purchase_order1.customer_id
          WHERE customer0.name = ?
        """,
        value = Vector("2018-02-13")
      )

      test("flatMap") - checker(
        Customer.query.flatMap(c => PurchaseOrder.query.map((c, _)))
          .filter{case (c, p) => c.id === p.customerId && c.name === "John Doe"}
          .map(_._2.orderDate)
      ).expect(
        sql = """
          SELECT purchase_order1.order_date as res
          FROM customer customer0, purchase_order purchase_order1
          WHERE customer0.id = purchase_order1.customer_id
          AND customer0.name = ?
        """,
        value = Vector("2018-02-13")
      )
      test("flatMap") - checker(
        Customer.query.flatMap(c =>
          PurchaseOrder.query
            .filter { p => c.id === p.customerId && c.name === "John Doe" }
        ).map(_.orderDate)

      ).expect(
        sql = """
          SELECT purchase_order1.order_date as res
          FROM customer customer0, purchase_order purchase_order1
          WHERE customer0.id = purchase_order1.customer_id
          AND customer0.name = ?
        """,
        value = Vector("2018-02-13")
      )
    }
  }
}

