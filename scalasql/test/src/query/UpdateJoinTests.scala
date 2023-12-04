package scalasql.query

import scalasql._
import sourcecode.Text
import utest._
import utils.ScalaSqlSuite

import java.time.LocalDate

trait UpdateJoinTests extends ScalaSqlSuite {
  def description = "`UPDATE` queries that use `JOIN`s"
  override def utestBeforeEach(path: Seq[String]): Unit = checker.reset()
  def tests = Tests {
    test("join") - {
      checker(
        query = Text {
          Buyer
            .update(_.name `=` "James Bond")
            .join(ShippingInfo)(_.id `=` _.buyerId)
            .set(c => c._1.dateOfBirth := c._2.shippingDate)
        },
        sqls = Seq(
          """
            UPDATE buyer
            SET date_of_birth = shipping_info0.shipping_date
            FROM shipping_info shipping_info0
            WHERE (buyer.id = shipping_info0.buyer_id) AND (buyer.name = ?)
          """,
          """
            UPDATE buyer
            JOIN shipping_info shipping_info0 ON (buyer.id = shipping_info0.buyer_id)
            SET buyer.date_of_birth = shipping_info0.shipping_date
            WHERE (buyer.name = ?)
          """
        ),
        value = 1,
        docs = """
          ScalaSql supports performing `UPDATE`s with `FROM`/`JOIN` clauses using the
          `.update.join` methods
        """
      )

      checker(
        query = Text { Buyer.select.filter(_.name `=` "James Bond").map(_.dateOfBirth) },
        value = Seq(LocalDate.parse("2012-04-05"))
      )
    }

    test("multijoin") - {
      checker(
        query = Text {
          Buyer
            .update(_.name `=` "James Bond")
            .join(ShippingInfo)(_.id `=` _.buyerId)
            .join(Purchase)(_._2.id `=` _.shippingInfoId)
            .join(Product)(_._3.productId `=` _.id)
            .filter(t => t._4.name.toLowerCase `=` t._4.kebabCaseName.toLowerCase)
            .set(c => c._1.name := c._4.name)
        },
        sqls = Seq(
          """
            UPDATE buyer
            SET name = product2.name
            FROM shipping_info shipping_info0
            JOIN purchase purchase1 ON (shipping_info0.id = purchase1.shipping_info_id)
            JOIN product product2 ON (purchase1.product_id = product2.id)
            WHERE (buyer.id = shipping_info0.buyer_id)
            AND (buyer.name = ?)
            AND (LOWER(product2.name) = LOWER(product2.kebab_case_name))
          """,
          """
            UPDATE buyer
            JOIN shipping_info shipping_info0 ON (buyer.id = shipping_info0.buyer_id)
            JOIN purchase purchase1 ON (shipping_info0.id = purchase1.shipping_info_id)
            JOIN product product2 ON (purchase1.product_id = product2.id)
            SET buyer.name = product2.name
            WHERE (buyer.name = ?)
            AND (LOWER(product2.name) = LOWER(product2.kebab_case_name))
          """
        ),
        value = 1,
        docs = """
          Multiple joins are supported, e.g. the below example where we join the `Buyer` table
          three times against `ShippingInfo`/`Purchase`/`Product` to determine what to update
        """
      )

      checker(query = Text { Buyer.select.filter(_.id `=` 1).map(_.name) }, value = Seq("Camera"))
    }

    test("joinSubquery") - {
      checker(
        query = Text {
          Buyer
            .update(_.name `=` "James Bond")
            .join(ShippingInfo.select.sortBy(_.id).asc.take(2))(_.id `=` _.buyerId)
            .set(c => c._1.dateOfBirth := c._2.shippingDate)
        },
        sqls = Seq(
          """
            UPDATE buyer SET date_of_birth = subquery0.shipping_date
            FROM (SELECT
                shipping_info0.id AS id,
                shipping_info0.buyer_id AS buyer_id,
                shipping_info0.shipping_date AS shipping_date
              FROM shipping_info shipping_info0
              ORDER BY id ASC
              LIMIT ?) subquery0
            WHERE (buyer.id = subquery0.buyer_id) AND (buyer.name = ?)
          """,
          """
            UPDATE
              buyer
              JOIN (SELECT
                  shipping_info0.id AS id,
                  shipping_info0.buyer_id AS buyer_id,
                  shipping_info0.shipping_date AS shipping_date
                FROM shipping_info shipping_info0
                ORDER BY id ASC
                LIMIT ?) subquery0 ON (buyer.id = subquery0.buyer_id)
            SET buyer.date_of_birth = subquery0.shipping_date
            WHERE (buyer.name = ?)
          """
        ),
        value = 1,
        docs = """
          In addition to `JOIN`ing against another table, you can also perform `JOIN`s against
          subqueries by passing in a `.select` query to `.join`
        """
      )

      checker(
        query = Text { Buyer.select.filter(_.name `=` "James Bond").map(_.dateOfBirth) },
        value = Seq(LocalDate.parse("2012-04-05"))
      )
    }
    test("joinSubqueryEliminatedColumn") - {
      checker(
        query = Text {
          Buyer
            .update(_.name `=` "James Bond")
            // Make sure the `SELECT shipping_info0.shipping_info_id AS shipping_info_id`
            // column gets eliminated since it is not used outside the subquery
            .join(ShippingInfo.select.sortBy(_.id).asc.take(2))(_.id `=` _.buyerId)
            .set(c => c._1.dateOfBirth := LocalDate.parse("2000-01-01"))
        },
        sqls = Seq(
          """
            UPDATE buyer SET date_of_birth = ?
            FROM (SELECT
                shipping_info0.id AS id,
                shipping_info0.buyer_id AS buyer_id
              FROM shipping_info shipping_info0
              ORDER BY id ASC
              LIMIT ?) subquery0
            WHERE (buyer.id = subquery0.buyer_id) AND (buyer.name = ?)
          """,
          """
            UPDATE
              buyer
              JOIN (SELECT
                  shipping_info0.id AS id,
                  shipping_info0.buyer_id AS buyer_id
                FROM shipping_info shipping_info0
                ORDER BY id ASC
                LIMIT ?) subquery0 ON (buyer.id = subquery0.buyer_id)
            SET buyer.date_of_birth = ?
            WHERE (buyer.name = ?)
          """
        ),
        value = 1
      )

      checker(
        query = Text { Buyer.select.filter(_.name `=` "James Bond").map(_.dateOfBirth) },
        value = Seq(LocalDate.parse("2000-01-01"))
      )
    }
  }
}
