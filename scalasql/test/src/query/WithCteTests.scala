package scalasql.query

import scalasql._
import sourcecode.Text
import utest._
import utils.ScalaSqlSuite

import java.time.LocalDate

trait WithCteTests extends ScalaSqlSuite {
  def description = "Basic `WITH`/Common-Table-Expression operations"

  def tests = Tests {

    test("simple") - checker(
      query = Text {
        db.withCte(Buyer.select.map(_.name)) { bs =>
          bs.map(_ + "-suffix")
        }
      },
      sqls = Seq(
        """
          WITH cte0 (res) AS (SELECT buyer0.name AS res FROM buyer buyer0)
          SELECT (cte0.res || ?) AS res
          FROM cte0
        """,
        """
          WITH cte0 (res) AS (SELECT buyer0.name AS res FROM buyer buyer0)
          SELECT CONCAT(cte0.res, ?) AS res
          FROM cte0
        """
      ),
      value = Seq("James Bond-suffix", "叉烧包-suffix", "Li Haoyi-suffix"),
      docs = """
        ScalaSql supports `WITH`-clauses, also known as "Common Table Expressions"
        (CTEs), via the `.withCte` syntax.
      """
    )

    test("multiple") - checker(
      query = Text {
        db.withCte(Buyer.select) { bs =>
          db.withCte(ShippingInfo.select) { sis =>
            bs.join(sis)(_.id === _.buyerId)
              .map { case (b, s) => (b.name, s.shippingDate) }
          }
        }
      },
      sql = """
        WITH
          cte0 (id, name) AS (SELECT
            buyer0.id AS id, buyer0.name AS name FROM buyer buyer0),
          cte1 (buyer_id, shipping_date) AS (SELECT
              shipping_info1.buyer_id AS buyer_id,
              shipping_info1.shipping_date AS shipping_date
            FROM shipping_info shipping_info1)
        SELECT cte0.name AS res_0, cte1.shipping_date AS res_1
        FROM cte0
        JOIN cte1 ON (cte0.id = cte1.buyer_id)
      """,
      value = Seq(
        ("叉烧包", LocalDate.parse("2010-02-03")),
        ("James Bond", LocalDate.parse("2012-04-05")),
        ("叉烧包", LocalDate.parse("2012-05-06"))
      ),
      docs = """
        Multiple `withCte` blocks can be stacked, turning into chained `WITH` clauses
        in the generated SQL
      """
    )

    test("eliminated") - checker(
      query = Text {
        db.withCte(Buyer.select) { bs =>
          bs.map(_.name + "-suffix")
        }
      },
      sqls = Seq(
        """
          WITH cte0 (name) AS (SELECT buyer0.name AS name FROM buyer buyer0)
          SELECT (cte0.name || ?) AS res
          FROM cte0
        """,
        """
          WITH cte0 (name) AS (SELECT buyer0.name AS name FROM buyer buyer0)
          SELECT CONCAT(cte0.name, ?) AS res
          FROM cte0
        """
      ),
      value = Seq("James Bond-suffix", "叉烧包-suffix", "Li Haoyi-suffix"),
      docs = """
        Only the necessary columns are exported from the `WITH` clause; columns that
        are un-used in the downstream `SELECT` clause are eliminated
      """
    )

    test("subquery") - checker(
      query = Text {
        db.withCte(Buyer.select) { bs =>
          db.withCte(ShippingInfo.select) { sis =>
            bs.join(sis)(_.id === _.buyerId)
          }
        }.join(
          db.withCte(Product.select) { prs =>
            Purchase.select.join(prs)(_.productId === _.id)
          }
        )(_._2.id === _._1.shippingInfoId)
          .map { case (b, s, (pu, pr)) => (b.name, pr.name) }
      },
      sql = """
        SELECT subquery0.res_0_name AS res_0, subquery1.res_1_name AS res_1
        FROM (WITH
            cte0 (id, name)
            AS (SELECT buyer0.id AS id, buyer0.name AS name FROM buyer buyer0),
            cte1 (id, buyer_id)
            AS (SELECT shipping_info1.id AS id, shipping_info1.buyer_id AS buyer_id
              FROM shipping_info shipping_info1)
          SELECT cte0.name AS res_0_name, cte1.id AS res_1_id
          FROM cte0
          JOIN cte1 ON (cte0.id = cte1.buyer_id)) subquery0
        JOIN (WITH
            cte1 (id, name)
            AS (SELECT product1.id AS id, product1.name AS name FROM product product1)
          SELECT
            purchase2.shipping_info_id AS res_0_shipping_info_id,
            cte1.name AS res_1_name
          FROM purchase purchase2
          JOIN cte1 ON (purchase2.product_id = cte1.id)) subquery1
        ON (subquery0.res_1_id = subquery1.res_0_shipping_info_id)
      """,
      value = Seq[(String, String)](
        ("James Bond", "Camera"),
        ("James Bond", "Skate Board"),
        ("叉烧包", "Cookie"),
        ("叉烧包", "Face Mask"),
        ("叉烧包", "Face Mask"),
        ("叉烧包", "Guitar"),
        ("叉烧包", "Socks")
      ),
      docs = """
        ScalaSql's `withCte` can be used anywhere a `.select` operator can be used. The
        generated `WITH` clauses may be wrapped in sub-queries in scenarios where they
        cannot be easily combined into a single query
      """,
      normalize = (x: Seq[(String, String)]) => x.sorted
    )
  }
}
