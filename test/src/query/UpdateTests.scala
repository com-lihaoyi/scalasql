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
      checker(Buyer.update.filter(_.name === "James Bond").set(_.birthdate -> "2019-04-07"))
        .expect(
          sql = "UPDATE buyer SET birthdate = ? WHERE buyer.name = ?",
          value = 1
        )

      checker(Buyer.select.filter(_.name === "James Bond").map(_.birthdate)).expect(
        value = Vector("2019-04-07")
      )

      checker(Buyer.select.filter(_.name === "Li Haoyi").map(_.birthdate)).expect(
        value = Vector("1965-08-09") // not updated
      )
    }

    test("bulk") - {
      checker(Buyer.update.set(_.birthdate -> "2019-04-07")).expect(
        sql = "UPDATE buyer SET birthdate = ?",
        value = 3
      )

      checker(Buyer.select.filter(_.name === "James Bond").map(_.birthdate)).expect(
        value = Vector("2019-04-07")
      )
      checker(Buyer.select.filter(_.name === "Li Haoyi").map(_.birthdate)).expect(
        value = Vector("2019-04-07")
      )
    }

    test("returning") - {
      checker(
        Buyer.update
          .filter(_.name === "James Bond")
          .set(_.birthdate -> "2019-04-07")
          .returning(_.id)
      ).expect(
        sql = "UPDATE buyer SET birthdate = ? WHERE buyer.name = ? RETURNING buyer.id as res",
        value = Vector(1)
      )

      checker(Buyer.select.filter(_.name === "James Bond").map(_.birthdate)).expect(
        value = Vector("2019-04-07")
      )
    }

    test("multiple") - {
      checker(
        Buyer.update
          .filter(_.name === "James Bond")
          .set(_.birthdate -> "2019-04-07", _.name -> "John Dee")
          .returning(_.id)
      ).expect(
        sql = "UPDATE buyer SET birthdate = ?, name = ? WHERE buyer.name = ? RETURNING buyer.id as res",
        value = Vector(1)
      )

      checker(Buyer.select.filter(_.name === "James Bond").map(_.birthdate)).expect(
        value = Nil
      )

      checker(Buyer.select.filter(_.name === "John Dee").map(_.birthdate)).expect(
        value = Vector("2019-04-07")
      )
    }

    test("returningMulti") - {
      checker(
        Buyer.update
          .filter(_.name === "James Bond")
          .set(_.birthdate -> "2019-04-07", _.name -> "John Dee")
          .returning(c => (c.id, c.name, c.birthdate))
      ).expect(
        sql = """
          UPDATE buyer
          SET birthdate = ?, name = ? WHERE buyer.name = ?
          RETURNING buyer.id as res__0, buyer.name as res__1, buyer.birthdate as res__2
        """,
        value = Vector((1, "John Dee", "2019-04-07"))
      )
    }

    test("dynamic") - {
      checker(
        Buyer.update
          .filter(_.name === "James Bond")
          .set(c => c.name -> c.name.toUpperCase)
          .returning(_.id)
      ).expect(
        sql = "UPDATE buyer SET name = UPPER(buyer.name) WHERE buyer.name = ? RETURNING buyer.id as res",
        value = Vector(1)
      )

      checker(Buyer.select.filter(_.name === "James Bond").map(_.birthdate)).expect(
        value = Nil
      )

      checker(Buyer.select.filter(_.name === "JAMES BOND").map(_.birthdate)).expect(
        value =  Vector("2001-02-03")
      )
    }

    test("join") - {
      checker(
        Buyer.update
          .filter(_.name === "James Bond")
          .joinOn(ShippingInfo)(_.id === _.buyerId)
          .set(c => c._1.birthdate -> c._2.shippingDate)
          .returning(_._1.id)
      ).expect(
        sql = """
          UPDATE buyer
          SET birthdate = shipping_info0.shipping_date
          FROM shipping_info shipping_info0
          WHERE buyer.id = shipping_info0.buyer_id AND buyer.name = ?
          RETURNING buyer.id as res
        """,
        value = Vector(1)
      )

      checker(Buyer.select.filter(_.name === "James Bond").map(_.birthdate)).expect(
        value = Vector("2012-04-05")
      )
    }

    test("multijoin") - {
      checker(
        Buyer.update
          .filter(_.name === "James Bond")
          .joinOn(ShippingInfo)(_.id === _.buyerId)
          .joinOn(Purchase)(_._2.id === _.shippingInfoId)
          .joinOn(Product)(_._2.productId === _.id)
          .set(c => c._1._1._1.name -> c._2.name)
          .returning(_._1._1._1.id)
      ).expect(
        sql = """
          UPDATE buyer
          SET name = product2.name
          FROM shipping_info shipping_info0
          JOIN purchase purchase1 ON shipping_info0.id = purchase1.shipping_info_id
          JOIN product product2 ON purchase1.product_id = product2.id
          WHERE buyer.id = shipping_info0.buyer_id AND buyer.name = ?
          RETURNING buyer.id as res
        """,
        value = Vector(1)
      )

      checker(Buyer.select.filter(_.id === 1).map(_.name)).expect(
        value = Vector("Camera")
      )
    }

    test("joinSubquery") - {
      checker(
        Buyer.update
          .filter(_.name === "James Bond")
          .joinOn(ShippingInfo.select.sortBy(_.id).asc.take(2))(_.id === _.buyerId)
          .set(c => c._1.birthdate -> c._2.shippingDate)
          .returning(_._1.id)
      ).expect(
        sql = """
          UPDATE buyer SET birthdate = subquery0.res__shipping_date
          FROM (SELECT
              shipping_info0.id as res__id,
              shipping_info0.buyer_id as res__buyer_id,
              shipping_info0.shipping_date as res__shipping_date
            FROM shipping_info shipping_info0
            ORDER BY shipping_info0.id ASC
            LIMIT 2) subquery0
          WHERE buyer.id = subquery0.res__buyer_id AND buyer.name = ?
          RETURNING buyer.id as res
        """,
        value = Vector(1)
      )

      checker(Buyer.select.filter(_.name === "James Bond").map(_.birthdate)).expect(
        value = Vector("2012-04-05")
      )
    }

    test("setSubquery") - {
      checker(
        Product.update
          .set(_.price -> Product.select.maxBy(_.price))
          .returning(p => (p.id, p.name, p.price))
      ).expect(
        sql = """
          UPDATE product
          SET price = (SELECT MAX(product0.price) as res FROM product product0)
          RETURNING
            product.id as res__0,
            product.name as res__1,
            product.price as res__2
        """,
        value = Vector(
          (1, "Face Mask", 1000.0),
          (2, "Guitar", 1000.0),
          (3, "Socks", 1000.0),
          (4, "Skateboard", 1000.0),
          (5, "Camera", 1000.0),
          (6, "Cookie", 1000.0)
        )
      )

      checker(Product.select.filter(_.name === "Face Mask").map(_.price)).expect(
        value = Vector(1000.0)
      )
    }

    test("whereSubquery") - {
      checker(
        Product.update
          .filter(_.price === Product.select.maxBy(_.price))
          .set(_.price -> 0)
          .returning(p => (p.name, p.price))
      ).expect(
        sql = """
          UPDATE product
          SET price = ?
          WHERE product.price = (SELECT MAX(product0.price) as res FROM product product0)
          RETURNING product.name as res__0, product.price as res__1
        """,
        value = Vector(("Camera", 0.0))
      )
    }
  }
}

