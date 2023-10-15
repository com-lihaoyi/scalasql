package usql.customers


import usql._
import ExprOps._
import utest._

/**
 * Tests for queries operations that force subqueries to be used.
 */
object SubQueryTests extends TestSuite {
  val checker = new TestDb("subquerytests")
  def tests = Tests {
    test("sortTakeJoin") - checker(
      Item.select
        .joinOn(Product.select.sortBy(_.price).desc.take(1))(_.productId === _.id)
        .map{case (item, product) => item.total}
    ).expect(
      sql = """
        SELECT item0.total as res
        FROM item item0
        JOIN (SELECT
            product0.id as res__id,
            product0.sku as res__sku,
            product0.name as res__name,
            product0.price as res__price
          FROM product product0
          ORDER BY product0.price DESC
          LIMIT 1) subquery1
        ON item0.product_id = subquery1.res__id
      """,
      value = Vector(15000.0)
    )

    test("sortTakeFrom") - checker(
      Product.select.sortBy(_.price).desc.take(1)
        .joinOn(Item.select)(_.id === _.productId)
        .map{case (product, item) => item.total}
    ).expect(
      sql = """
        SELECT item1.total as res
        FROM (SELECT
            product0.id as res__id,
            product0.sku as res__sku,
            product0.name as res__name,
            product0.price as res__price
          FROM product product0
          ORDER BY product0.price DESC
          LIMIT 1) subquery0
        JOIN item item1 ON subquery0.res__id = item1.product_id
      """,
      value = Vector(15000.0)
    )

    test("sortTakeFromAndJoin") - checker(
      Product.select.sortBy(_.price).desc.take(3)
        .joinOn(Item.select.sortBy(_.quantity).desc.take(3))(_.id === _.productId)
        .map{case (product, item) => (product.name, item.quantity) }
    ).expect(
      sql = """
        SELECT
          subquery0.res__name as res__0,
          subquery1.res__quantity as res__1
        FROM (SELECT
            product0.id as res__id,
            product0.sku as res__sku,
            product0.name as res__name,
            product0.price as res__price
          FROM product product0
          ORDER BY product0.price DESC
          LIMIT 3) subquery0
        JOIN (SELECT
            item0.id as res__id,
            item0.order_id as res__order_id,
            item0.product_id as res__product_id,
            item0.quantity as res__quantity,
            item0.total as res__total
          FROM item item0
          ORDER BY item0.quantity DESC
          LIMIT 3) subquery1
        ON subquery0.res__id = subquery1.res__product_id
      """,
      value = Vector(("Cell Phone", 15))
    )

    test("sortLimitSortLimit") - checker(
      Product.select.sortBy(_.price).desc.take(4).sortBy(_.price).asc.take(2).map(_.name)
    ).expect(
      sql = """
        SELECT subquery0.res__name as res
        FROM (SELECT
            product0.id as res__id,
            product0.sku as res__sku,
            product0.name as res__name,
            product0.price as res__price
          FROM product product0
          ORDER BY product0.price DESC
          LIMIT 4) subquery0
        ORDER BY subquery0.res__price ASC
        LIMIT 2
      """,
      value = Vector("Keyboard", "Bed")
    )

    test("sortGroupBy") - checker(
      Item.select.sortBy(_.quantity).take(5).groupBy(_.productId)(_.sumBy(_.total))
    ).expect(
      sql = """
        SELECT subquery0.res__product_id as res__0, SUM(subquery0.res__total) as res__1
        FROM (SELECT
            item0.id as res__id,
            item0.order_id as res__order_id,
            item0.product_id as res__product_id,
            item0.quantity as res__quantity,
            item0.total as res__total
          FROM item item0
          ORDER BY item0.quantity
          LIMIT 5) subquery0
        GROUP BY subquery0.res__product_id
      """,
      value = Vector((1, 135.83), (2, 703.92), (3, 24.99), (4, 262.0))
    )

    test("groupByJoin") - checker(
      Item.select.groupBy(_.productId)(_.sumBy(_.total)).joinOn(Product.select)(_._1 === _.id)
        .map{case ((productId, total), product) => (product.name, total)}
    ).expect(
      sql = """
        SELECT
          product1.name as res__0,
          subquery0.res__1 as res__1
        FROM (SELECT
            item0.product_id as res__0,
            SUM(item0.total) as res__1
          FROM item item0
          GROUP BY item0.product_id) subquery0
        JOIN product product1 ON subquery0.res__0 = product1.id
      """,
      value = Vector(
        ("Keyboard", 135.83),
        ("Television", 703.92),
        ("Shirt", 24.99),
        ("Bed", 262.0),
        ("Cell Phone", 15000.0),
        ("Spoon", 18.0)
      )
    )

    test("subqueryInFilter") - checker(
      Customer.select.filter(c => PurchaseOrder.select.filter(p => c.id === p.customerId).size === 0)
    ).expect(
      sql =
        """
        SELECT
          customer0.id as res__id,
          customer0.name as res__name,
          customer0.birthdate as res__birthdate
        FROM customer customer0
        WHERE (SELECT
            COUNT(1) as res
            FROM purchase_order purchase_order0
            WHERE customer0.id = purchase_order0.customer_id) = ?
      """,
      value = Vector(Customer(3, "Cosme Fulanito", "1956-05-12"))
    )
    test("subqueryInMap") - checker(
      Customer.select.map(c => (c, PurchaseOrder.select.filter(p => c.id === p.customerId).size))
    ).expect(
      sql =
        """
        SELECT
          customer0.id as res__0__id,
          customer0.name as res__0__name,
          customer0.birthdate as res__0__birthdate,
          (SELECT COUNT(1) as res FROM purchase_order purchase_order0 WHERE customer0.id = purchase_order0.customer_id) as res__1
        FROM customer customer0
      """,
      value = Vector(
        (Customer(1, "John Doe", "1960-10-30"), 1),
        (Customer(2, "Pepito Pérez", "1954-07-15"), 2),
        (Customer(3, "Cosme Fulanito", "1956-05-12"), 0)
      )
    )
    test("subqueryInMapNested") - checker(
      Customer.select.map(c => (c, PurchaseOrder.select.filter(p => c.id === p.customerId).size === 1))
    ).expect(
      sql =
        """
        SELECT
          customer0.id as res__0__id,
          customer0.name as res__0__name,
          customer0.birthdate as res__0__birthdate,
          (SELECT COUNT(1) as res FROM purchase_order purchase_order0 WHERE customer0.id = purchase_order0.customer_id) = ? as res__1
        FROM customer customer0
      """,
      value = Vector(
        (Customer(1, "John Doe", "1960-10-30"), true),
        (Customer(2, "Pepito Pérez", "1954-07-15"), false),
        (Customer(3, "Cosme Fulanito", "1956-05-12"), false)
      )
    )
  }
}

