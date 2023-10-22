package usql.query

import usql._
import utest._

import java.sql.Date

/**
 * Tests for basic update operations
 */
trait UpdateTests extends UsqlTestSuite {
  override def utestBeforeEach(path: Seq[String]): Unit = checker.reset()
  def tests = Tests {
    test("update") - {
      checker(
        query = Buyer.update.filter(_.name === "James Bond").set(
          _.dateOfBirth -> Date.valueOf("2019-04-07")
        ),
        sqls =
          Seq(
            "UPDATE buyer SET date_of_birth = ? WHERE buyer.name = ?",
            "UPDATE buyer SET buyer.date_of_birth = ? WHERE buyer.name = ?"
          ),
        value = 1
      )

      checker(
        query = Buyer.select.filter(_.name === "James Bond").map(_.dateOfBirth),
        value = Seq(Date.valueOf("2019-04-07"))
      )

      checker(
        query = Buyer.select.filter(_.name === "Li Haoyi").map(_.dateOfBirth),
        value = Seq(Date.valueOf("1965-08-09")) // not updated
      )
    }

    test("bulk") - {
      checker(
        query = Buyer.update.set(_.dateOfBirth -> Date.valueOf("2019-04-07")),
        sqls = Seq(
          "UPDATE buyer SET date_of_birth = ?",
          "UPDATE buyer SET buyer.date_of_birth = ?"
        ),
        value = 3
      )

      checker(
        query = Buyer.select.filter(_.name === "James Bond").map(_.dateOfBirth),
        value = Seq(Date.valueOf("2019-04-07"))
      )
      checker(
        query = Buyer.select.filter(_.name === "Li Haoyi").map(_.dateOfBirth),
        value = Seq(Date.valueOf("2019-04-07"))
      )
    }

    test("multiple") - {
      checker(
        query =
          Buyer.update
            .filter(_.name === "James Bond")
            .set(_.dateOfBirth -> Date.valueOf("2019-04-07"), _.name -> "John Dee"),
        sqls = Seq(
          "UPDATE buyer SET date_of_birth = ?, name = ? WHERE buyer.name = ?",
          "UPDATE buyer SET buyer.date_of_birth = ?, buyer.name = ? WHERE buyer.name = ?"
        ),
        value = 1
      )

      checker(
        query = Buyer.select.filter(_.name === "James Bond").map(_.dateOfBirth),
        value = Seq[Date]()
      )

      checker(
        query = Buyer.select.filter(_.name === "John Dee").map(_.dateOfBirth),
        value = Seq(Date.valueOf("2019-04-07"))
      )
    }

    test("dynamic") - {
      checker(
        query =
          Buyer.update
            .filter(_.name === "James Bond")
            .set(c => c.name -> c.name.toUpperCase),
        sqls = Seq(
          "UPDATE buyer SET name = UPPER(buyer.name) WHERE buyer.name = ?",
          "UPDATE buyer SET buyer.name = UPPER(buyer.name) WHERE buyer.name = ?"
        ),
        value = 1
      )

      checker(
        query = Buyer.select.filter(_.name === "James Bond").map(_.dateOfBirth),
        value = Seq[Date]()
      )

      checker(
        query = Buyer.select.filter(_.name === "JAMES BOND").map(_.dateOfBirth),
        value = Seq(Date.valueOf("2001-02-03"))
      )
    }

    test("join") - {
      checker(
        query =
          Buyer.update
            .filter(_.name === "James Bond")
            .joinOn(ShippingInfo)(_.id === _.buyerId)
            .set(c => c._1.dateOfBirth -> c._2.shippingDate),
        sqls = Seq(
          """
            UPDATE buyer
            SET date_of_birth = shipping_info0.shipping_date
            FROM shipping_info shipping_info0
            WHERE buyer.id = shipping_info0.buyer_id AND buyer.name = ?
          """,
          """
            UPDATE buyer
            JOIN shipping_info shipping_info0 ON buyer.id = shipping_info0.buyer_id
            SET buyer.date_of_birth = shipping_info0.shipping_date
            WHERE buyer.name = ?
          """
        ),
        value = 1
      )

      checker(
        query = Buyer.select.filter(_.name === "James Bond").map(_.dateOfBirth),
        value = Seq(Date.valueOf("2012-04-05"))
      )
    }

    test("multijoin") - {
      checker(
        query =
          Buyer.update
            .filter(_.name === "James Bond")
            .joinOn(ShippingInfo)(_.id === _.buyerId)
            .joinOn(Purchase)(_._2.id === _.shippingInfoId)
            .joinOn(Product)(_._2.productId === _.id)
            .filter(t => t._2.name.toLowerCase === t._2.kebabCaseName.toLowerCase)
            .set(c => c._1._1._1.name -> c._2.name),
        sqls = Seq(
          """
            UPDATE buyer
            SET name = product2.name
            FROM shipping_info shipping_info0
            JOIN purchase purchase1 ON shipping_info0.id = purchase1.shipping_info_id
            JOIN product product2 ON purchase1.product_id = product2.id
            WHERE buyer.id = shipping_info0.buyer_id
            AND buyer.name = ?
            AND LOWER(product2.name) = LOWER(product2.kebab_case_name)
          """,
          """
            UPDATE buyer
            JOIN shipping_info shipping_info0 ON buyer.id = shipping_info0.buyer_id
            JOIN purchase purchase1 ON shipping_info0.id = purchase1.shipping_info_id
            JOIN product product2 ON purchase1.product_id = product2.id
            SET buyer.name = product2.name
            WHERE buyer.name = ?
            AND LOWER(product2.name) = LOWER(product2.kebab_case_name)
          """
        ),
        value = 1
      )

      checker(
        query = Buyer.select.filter(_.id === 1).map(_.name),
        value = Seq("Camera")
      )
    }

    test("joinSubquery") - {
      checker(
        query =
          Buyer.update
            .filter(_.name === "James Bond")
            .joinOn(ShippingInfo.select.sortBy(_.id).asc.take(2))(_.id === _.buyerId)
            .set(c => c._1.dateOfBirth -> c._2.shippingDate),
        sqls = Seq(
          """
            UPDATE buyer SET date_of_birth = subquery0.res__shipping_date
            FROM (SELECT
                shipping_info0.id as res__id,
                shipping_info0.buyer_id as res__buyer_id,
                shipping_info0.shipping_date as res__shipping_date
              FROM shipping_info shipping_info0
              ORDER BY res__id ASC
              LIMIT 2) subquery0
            WHERE buyer.id = subquery0.res__buyer_id AND buyer.name = ?
          """,
          """
            UPDATE
              buyer
              JOIN (SELECT
                  shipping_info0.id as res__id,
                  shipping_info0.buyer_id as res__buyer_id,
                  shipping_info0.shipping_date as res__shipping_date
                FROM shipping_info shipping_info0
                ORDER BY res__id ASC
                LIMIT 2) subquery0 ON buyer.id = subquery0.res__buyer_id
            SET buyer.date_of_birth = subquery0.res__shipping_date
            WHERE buyer.name = ?
          """
        ),
        value = 1
      )

      checker(
        query = Buyer.select.filter(_.name === "James Bond").map(_.dateOfBirth),
        value = Seq(Date.valueOf("2012-04-05"))
      )
    }

    test("setSubquery") - {
      checker(
        query =
          Product.update
            .set(_.price -> Product.select.maxBy(_.price)),
        sqls = Seq(
          """
            UPDATE product
            SET price = (SELECT MAX(product0.price) as res FROM product product0)
          """,
          """
            UPDATE product
            SET product.price = (SELECT MAX(product0.price) as res FROM product product0)
          """
        ),
        value = 6
      )

      checker(
        query = Product.select.map(p => (p.id, p.name, p.price)),
        value = Seq(
          (1, "Face Mask", 1000.0),
          (2, "Guitar", 1000.0),
          (3, "Socks", 1000.0),
          (4, "Skate Board", 1000.0),
          (5, "Camera", 1000.0),
          (6, "Cookie", 1000.0)
        )
      )
    }

    test("whereSubquery") - {
      checker(
        query =
          Product.update
            .filter(_.price === Product.select.maxBy(_.price))
            .set(_.price -> 0),
        sqls = Seq(
          """
            UPDATE product
            SET price = ?
            WHERE product.price = (SELECT MAX(product0.price) as res FROM product product0)
          """,
          """
            UPDATE product
            SET product.price = ?
            WHERE product.price = (SELECT MAX(product0.price) as res FROM product product0)
          """
        ),
        value = 1
      )

      checker(
        query = Product.select.map(p => (p.id, p.name, p.price)),
        value = Seq(
          (1, "Face Mask", 8.88),
          (2, "Guitar", 300.0),
          (3, "Socks", 3.14),
          (4, "Skate Board", 123.45),
          (5, "Camera", 0.0),
          (6, "Cookie", 0.1)
        ),
        normalize = (x: Seq[(Int, String, Double)]) => x.sorted
      )

    }
  }
}
