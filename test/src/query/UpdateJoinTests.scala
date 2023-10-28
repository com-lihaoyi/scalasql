package scalasql.query

import scalasql._
import utest._

import java.time.LocalDate

/**
 * Tests for basic update operations
 */
trait UpdateJoinTests extends ScalaSqlSuite {
  override def utestBeforeEach(path: Seq[String]): Unit = checker.reset()
  def tests = Tests {
    test("join") - {
      checker(
        query = Buyer.update.filter(_.name === "James Bond")
          .joinOn(ShippingInfo)(_.id === _.buyerId).set(c => c._1.dateOfBirth -> c._2.shippingDate),
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
        value = Seq(LocalDate.parse("2012-04-05"))
      )
    }

    test("multijoin") - {
      checker(
        query = Buyer.update.filter(_.name === "James Bond")
          .joinOn(ShippingInfo)(_.id === _.buyerId).joinOn(Purchase)(_._2.id === _.shippingInfoId)
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

      checker(query = Buyer.select.filter(_.id === 1).map(_.name), value = Seq("Camera"))
    }

    test("joinSubquery") - {
      checker(
        query = Buyer.update.filter(_.name === "James Bond")
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
        value = Seq(LocalDate.parse("2012-04-05"))
      )
    }
  }
}
