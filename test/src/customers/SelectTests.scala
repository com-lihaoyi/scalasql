package usql.customers

import utest._
import usql._
import ExprOps._
import usql.query.Expr

/**
 * Tests for basic query operations: map, filter, join, etc.
 */
object SelectTests extends TestSuite {
  val checker = new TestDb("querytests")
  def tests = Tests {
    test("constant") - checker(Expr(1)).expect(
      sql = "SELECT ? as res",
      value = 1
    )

    test("table") - checker(Customer.select).expect(
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
      test("single") - checker(PurchaseOrder.select.filter(_.customerId === 2)).expect(
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
        PurchaseOrder.select.filter(_.customerId === 2).filter(_.orderDate === "2018-02-25")
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
        PurchaseOrder.select.filter(p => p.customerId === 2 && p.orderDate === "2018-02-25")
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
      test("single") - checker(Customer.select.map(_.name)).expect(
        sql = "SELECT customer0.name as res FROM customer customer0",
        value = Vector("John Doe", "Pepito Pérez", "Cosme Fulanito")
      )

      test("tuple2") - checker(Customer.select.map(c => (c.name, c.id))).expect(
        sql = "SELECT customer0.name as res__0, customer0.id as res__1 FROM customer customer0",
        value =  Vector(("John Doe", 1), ("Pepito Pérez", 2), ("Cosme Fulanito", 3))
      )

      test("tuple3") - checker(Customer.select.map(c => (c.name, c.id, c.birthdate))).expect(
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

      test("interpolateInMap") - checker(Product.select.map(_.price * 2)).expect(
        sql = "SELECT product0.price * ? as res FROM product product0",
        value = Vector(15.98, 703.92, 7.14, 262.0, 2000.0, 2.0)
      )

      test("heterogenousTuple") - checker(Customer.select.map(c => (c.id, c))).expect(
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

    test("filterMap") - checker(Product.select.filter(_.price < 100).map(_.name)).expect(
      sql = "SELECT product0.name as res FROM product product0 WHERE product0.price < ?",
      value = Vector("Keyboard", "Shirt", "Spoon")
    )

    test("aggregate"){
      test("single") - checker(
        Item.select.aggregate(_.sumBy(_.total))
      ).expect(
        sql = "SELECT SUM(item0.total) as res FROM item item0",
        value = 16144.74
      )
      test("multiple") - checker(
        Item.select.aggregate(q => (q.sumBy(_.total), q.maxBy(_.total)))
      ).expect(
        sql = "SELECT SUM(item0.total) as res__0, MAX(item0.total) as res__1 FROM item item0",
        value = (16144.74, 15000.0)
      )
    }

    test("groupBy") - {
      test("simple") - checker(
        Item.select.groupBy(_.productId)(_.sumBy(_.total))
      ).expect(
        sql = """
          SELECT item0.product_id as res__0, SUM(item0.total) as res__1
          FROM item item0
          GROUP BY item0.product_id
        """,
        value =  Vector((1, 135.83), (2, 703.92), (3, 24.99), (4, 262.0), (5, 15000.0), (6, 18.0))
      )

      test("having") - checker(
        Item.select.groupBy(_.productId)(_.sumBy(_.total)).filter(_._2 > 100).filter(_._1 > 1)
      ).expect(
        sql = """
          SELECT item0.product_id as res__0, SUM(item0.total) as res__1
          FROM item item0
          GROUP BY item0.product_id
          HAVING SUM(item0.total) > ? AND item0.product_id > ?
        """,
        value = Vector((2, 703.92), (4, 262.0), (5, 15000.0))
      )

      test("filterHaving") - checker(
        Item.select.filter(_.quantity > 5).groupBy(_.productId)(_.sumBy(_.total)).filter(_._2 > 100)
      ).expect(
        sql = """
          SELECT item0.product_id as res__0, SUM(item0.total) as res__1
          FROM item item0
          WHERE item0.quantity > ?
          GROUP BY item0.product_id
          HAVING SUM(item0.total) > ?
        """,
        value = Vector((1, 135.83), (5, 15000.0))
      )
    }

    test("sort") {
      test("sort") - checker(Product.select.sortBy(_.price).map(_.name)).expect(
        sql = "SELECT product0.name as res FROM product product0 ORDER BY product0.price",
        value = Vector("Spoon", "Shirt", "Keyboard", "Bed", "Television", "Cell Phone")
      )

      test("sortLimit") - checker(Product.select.sortBy(_.price).map(_.name).take(2)).expect(
        sql = "SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 2",
        value = Vector("Spoon", "Shirt")
      )

      test("sortLimitTwiceHigher") - checker(Product.select.sortBy(_.price).map(_.name).take(2).take(3)).expect(
        sql = "SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 2",
        value = Vector("Spoon", "Shirt")
      )

      test("sortLimitTwiceLower") - checker(Product.select.sortBy(_.price).map(_.name).take(2).take(1)).expect(
        sql = "SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 1",
        value = Vector("Spoon")
      )

      test("sortLimitOffset") - checker(Product.select.sortBy(_.price).map(_.name).drop(2).take(2)).expect(
        sql = "SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 2 OFFSET 2",
        value = Vector("Keyboard", "Bed")
      )

      test("sortLimitOffsetTwice") - checker(Product.select.sortBy(_.price).map(_.name).drop(2).drop(2).take(1)).expect(
        sql = "SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 1 OFFSET 4",
        value = Vector("Television")
      )

      test("sortOffsetLimit") - checker(Product.select.sortBy(_.price).map(_.name).drop(2).take(2)).expect(
        sql = "SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 2 OFFSET 2",
        value = Vector("Keyboard", "Bed")
      )

      test("sortLimitOffset") - checker(Product.select.sortBy(_.price).map(_.name).take(2).drop(1)).expect(
        sql = "SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 1 OFFSET 1",
        value = Vector("Shirt")
      )
    }

    test("joins"){
      test("joinFilter") - checker(
        Customer.select.joinOn(PurchaseOrder)(_.id === _.customerId)
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

      test("joinSelectFilter") - checker(
        Customer.select.joinOn(PurchaseOrder.select)(_.id === _.customerId)
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
        Customer.select.joinOn(PurchaseOrder)(_.id === _.customerId)
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
        Customer.select.flatMap(c => PurchaseOrder.select.map((c, _)))
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
        Customer.select.flatMap(c =>
          PurchaseOrder.select
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

//    test("union")  - ???
//    test("unionAll") - ???
    test("distinct"){
      test("nondistinct") - checker(
        Item.select.map(_.orderId)
      ).expect(
        sql = "SELECT item0.order_id as res FROM item item0",
        value = Vector(1, 1, 1, 2, 2, 3, 3)
      )

      test("distinct") - checker(Item.select.map(_.orderId).distinct).expect(
        sql = "SELECT DISTINCT item0.order_id as res FROM item item0",
        value = Vector(1, 2, 3)
      )
    }
//    test("distinct on") - ???

//    test("nonEmpty") - ???
//    test("nested") - ???
  }
}

