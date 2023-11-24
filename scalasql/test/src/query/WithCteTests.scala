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
        withCte(Buyer.select.map(_.name)) { bs =>
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
        withCte(Buyer.select) { bs =>
          withCte(ShippingInfo.select) { sis =>
            bs.join(sis)(_.id === _.buyerId)
              .map { case (b, s) => (b.name, s.shippingDate) }
          }
        }
      },
      sql = """
        WITH
          cte0 (res__id, res__name) AS (SELECT
            buyer0.id AS res__id, buyer0.name AS res__name FROM buyer buyer0),
          cte1 (res__buyerId, res__shippingDate) AS (SELECT
              shipping_info1.buyer_id AS res__buyer_id,
              shipping_info1.shipping_date AS res__shipping_date
            FROM shipping_info shipping_info1)
        SELECT cte0.res__name AS res__0, cte1.res__shippingDate AS res__1
        FROM cte0
        JOIN cte1 ON (cte0.res__id = cte1.res__buyerId)
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
        withCte(Buyer.select) { bs =>
          bs.map(_.name + "-suffix")
        }
      },
      sqls = Seq(
        """
          WITH cte0 (res__name) AS (SELECT buyer0.name AS res__name FROM buyer buyer0)
          SELECT (cte0.res__name || ?) AS res
          FROM cte0
        """,
        """
          WITH cte0 (res__name) AS (SELECT buyer0.name AS res__name FROM buyer buyer0)
          SELECT CONCAT(cte0.res__name, ?) AS res
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
        withCte(Buyer.select) { bs =>
          withCte(ShippingInfo.select) { sis =>
            bs.join(sis)(_.id === _.buyerId)
          }
        }.join(
          withCte(Product.select) { prs =>
            Purchase.select.join(prs)(_.productId === _.id)
          }
        )(_._2.id === _._1.shippingInfoId)
          .map { case ((b, s), (pu, pr)) => (b.name, pr.name) }
      },
      sql = """
        SELECT subquery0.res__0__name AS res__0, subquery1.res__1__name AS res__1
        FROM (WITH
            cte0 (res__id, res__name)
            AS (SELECT buyer0.id AS res__id, buyer0.name AS res__name FROM buyer buyer0),
            cte1 (res__id, res__buyerId)
            AS (SELECT shipping_info1.id AS res__id, shipping_info1.buyer_id AS res__buyer_id
              FROM shipping_info shipping_info1)
          SELECT cte0.res__name AS res__0__name, cte1.res__id AS res__1__id
          FROM cte0
          JOIN cte1 ON (cte0.res__id = cte1.res__buyerId)) subquery0
        JOIN (WITH
            cte1 (res__id, res__name)
            AS (SELECT product1.id AS res__id, product1.name AS res__name FROM product product1)
          SELECT
            purchase2.shipping_info_id AS res__0__shipping_info_id,
            cte1.res__name AS res__1__name
          FROM purchase purchase2
          JOIN cte1 ON (purchase2.product_id = cte1.res__id)) subquery1
        ON (subquery0.res__1__id = subquery1.res__0__shipping_info_id)
      """,
      value = Seq[(String, String)](
        ("叉烧包", "Face Mask"),
        ("叉烧包", "Guitar"),
        ("叉烧包", "Socks"),
        ("James Bond", "Skate Board"),
        ("James Bond", "Camera"),
        ("叉烧包", "Face Mask"),
        ("叉烧包", "Cookie")
      ),
      docs = """
        ScalaSql's `withCte` can be used anywhere a `.select` operator can be used. The
        generated `WITH` clauses may be wrapped in sub-queries in scenarios where they
        cannot be easily combined into a single query
      """
    )
  }
}
