package scalasql.query

import scalasql._
import sourcecode.Text
import utest._
import utils.ScalaSqlSuite

import java.time.LocalDate

trait SubQueryTests extends ScalaSqlSuite {
  def description =
    "Queries that explicitly use subqueries (e.g. for `JOIN`s) or require subqueries " +
      "to preserve the Scala semantics of the various operators"

  def tests = Tests {
    test("sortTakeJoin") - checker(
      query = Text {
        Purchase.select
          .joinOn(Product.select.sortBy(_.price).desc.take(1))(_.productId `=` _.id)
          .map { case (purchase, product) => purchase.total }
      },
      sql = """
        SELECT purchase0.total as res
        FROM purchase purchase0
        JOIN (SELECT product0.id as res__id, product0.price as res__price
          FROM product product0
          ORDER BY res__price DESC
          LIMIT 1) subquery1
        ON purchase0.product_id = subquery1.res__id
      """,
      value = Seq(10000.0),
      docs = """
        A ScalaSql `.joinOn` referencing a `.select` translates straightforwardly
        into a SQL `JOIN` on a subquery
      """
    )

    test("sortTakeFrom") - checker(
      query = Text {
        Product.select.sortBy(_.price).desc.take(1).joinOn(Purchase)(_.id `=` _.productId).map {
          case (product, purchase) => purchase.total
        }
      },
      sql = """
        SELECT purchase1.total as res
        FROM (SELECT product0.id as res__id, product0.price as res__price
          FROM product product0
          ORDER BY res__price DESC
          LIMIT 1) subquery0
        JOIN purchase purchase1 ON subquery0.res__id = purchase1.product_id
      """,
      value = Seq(10000.0),
      docs = """
        Some sequences of operations cannot be expressed as a single SQL query,
        and thus translate into an outer query wrapping a subquery inside the `FROM`.
        An example of this is performing a `.joinOn` after a `.take`: SQL does not
        allow you to put `JOIN`s after `LIMIT`s, and so the only way to write this
        in SQL is as a subquery.
      """
    )

    test("sortTakeFromAndJoin") - checker(
      query = Text {
        Product.select
          .sortBy(_.price)
          .desc
          .take(3)
          .joinOn(Purchase.select.sortBy(_.count).desc.take(3))(_.id `=` _.productId)
          .map { case (product, purchase) => (product.name, purchase.count) }
      },
      sql = """
        SELECT
          subquery0.res__name as res__0,
          subquery1.res__count as res__1
        FROM (SELECT
            product0.id as res__id,
            product0.name as res__name,
            product0.price as res__price
          FROM product product0
          ORDER BY res__price DESC
          LIMIT 3) subquery0
        JOIN (SELECT
            purchase0.product_id as res__product_id,
            purchase0.count as res__count
          FROM purchase purchase0
          ORDER BY res__count DESC
          LIMIT 3) subquery1
        ON subquery0.res__id = subquery1.res__product_id
      """,
      value = Seq(("Camera", 10)),
      docs = """
        This example shows a ScalaSql query that results in a subquery in both
        the `FROM` and the `JOIN` clause of the generated SQL query.
      """
    )

    test("sortLimitSortLimit") - checker(
      query = Text {
        Product.select.sortBy(_.price).desc.take(4).sortBy(_.price).asc.take(2).map(_.name)
      },
      sql = """
        SELECT subquery0.res__name as res
        FROM (SELECT
            product0.name as res__name,
            product0.price as res__price
          FROM product product0
          ORDER BY res__price DESC
          LIMIT 4) subquery0
        ORDER BY subquery0.res__price ASC
        LIMIT 2
      """,
      value = Seq("Face Mask", "Skate Board"),
      docs = """
        Performing multiple sorts with `.take`s in between is also something
        that requires subqueries, as a single query only allows a single `LIMIT`
        clause after the `ORDER BY`
      """
    )

    test("sortGroupBy") - checker(
      query = Text {
        Purchase.select.sortBy(_.count).take(5).groupBy(_.productId)(_.sumBy(_.total))
      },
      sql = """
        SELECT subquery0.res__product_id as res__0, SUM(subquery0.res__total) as res__1
        FROM (SELECT
            purchase0.product_id as res__product_id,
            purchase0.count as res__count,
            purchase0.total as res__total
          FROM purchase purchase0
          ORDER BY res__count
          LIMIT 5) subquery0
        GROUP BY subquery0.res__product_id
      """,
      value = Seq((1, 44.4), (2, 900.0), (3, 15.7), (4, 493.8), (5, 10000.0)),
      normalize = (x: Seq[(Int, Double)]) => x.sorted
    )

    test("groupByJoin") - checker(
      query = Text {
        Purchase.select.groupBy(_.productId)(_.sumBy(_.total)).joinOn(Product)(_._1 `=` _.id).map {
          case ((productId, total), product) => (product.name, total)
        }
      },
      sql = """
        SELECT
          product1.name as res__0,
          subquery0.res__1 as res__1
        FROM (SELECT
            purchase0.product_id as res__0,
            SUM(purchase0.total) as res__1
          FROM purchase purchase0
          GROUP BY purchase0.product_id) subquery0
        JOIN product product1 ON subquery0.res__0 = product1.id
      """,
      value = Seq(
        ("Camera", 10000.0),
        ("Cookie", 1.3),
        ("Face Mask", 932.4),
        ("Guitar", 900.0),
        ("Skate Board", 493.8),
        ("Socks", 15.7)
      ),
      normalize = (x: Seq[(String, Double)]) => x.sorted
    )

    test("subqueryInFilter") - checker(
      query = Text {
        Buyer.select.filter(c => ShippingInfo.select.filter(p => c.id `=` p.buyerId).size `=` 0)
      },
      sql = """
        SELECT
          buyer0.id as res__id,
          buyer0.name as res__name,
          buyer0.date_of_birth as res__date_of_birth
        FROM buyer buyer0
        WHERE (SELECT
            COUNT(1) as res
            FROM shipping_info shipping_info0
            WHERE buyer0.id = shipping_info0.buyer_id) = ?
      """,
      value = Seq(Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09"))),
      docs = """
        You can use `.select`s and aggregate operations like `.size` anywhere an expression is
        expected; these translate into SQL subqueries as expressions. SQL
        subqueries-as-expressions require that the subquery returns exactly 1 row and 1 column,
        which is something the aggregate operation (in this case `.sum`/`COUNT(1)`) helps us
        ensure. Here, we do subquery in a `.filter`/`WHERE`.
      """
    )
    test("subqueryInMap") - checker(
      query = Text {
        Buyer.select.map(c => (c, ShippingInfo.select.filter(p => c.id `=` p.buyerId).size))
      },
      sql = """
        SELECT
          buyer0.id as res__0__id,
          buyer0.name as res__0__name,
          buyer0.date_of_birth as res__0__date_of_birth,
          (SELECT COUNT(1) as res FROM shipping_info shipping_info0 WHERE buyer0.id = shipping_info0.buyer_id) as res__1
        FROM buyer buyer0
      """,
      value = Seq(
        (Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")), 1),
        (Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")), 2),
        (Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09")), 0)
      ),
      docs = """
        Similar to the above example, but we do the subquery/aggregate in
        a `.map` instead of a `.filter`
      """
    )
    test("subqueryInMapNested") - checker(
      query = Text {
        Buyer.select.map(c => (c, ShippingInfo.select.filter(p => c.id `=` p.buyerId).size `=` 1))
      },
      sql = """
        SELECT
          buyer0.id as res__0__id,
          buyer0.name as res__0__name,
          buyer0.date_of_birth as res__0__date_of_birth,
          (SELECT
            COUNT(1) as res
            FROM shipping_info shipping_info0
            WHERE buyer0.id = shipping_info0.buyer_id) = ? as res__1
        FROM buyer buyer0
      """,
      value = Seq(
        (Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")), true),
        (Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")), false),
        (Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09")), false)
      )
    )

    test("selectLimitUnionSelect") - checker(
      query = Text {
        Buyer.select
          .map(_.name.toLowerCase)
          .take(2)
          .unionAll(Product.select.map(_.kebabCaseName.toLowerCase))
      },
      sql = """
        SELECT subquery0.res as res
        FROM (SELECT
            LOWER(buyer0.name) as res
          FROM buyer buyer0
          LIMIT 2) subquery0
        UNION ALL
        SELECT LOWER(product0.kebab_case_name) as res
        FROM product product0
      """,
      value =
        Seq("james bond", "叉烧包", "face-mask", "guitar", "socks", "skate-board", "camera", "cookie")
    )

    test("selectUnionSelectLimit") - checker(
      query = Text {
        Buyer.select
          .map(_.name.toLowerCase)
          .unionAll(Product.select.map(_.kebabCaseName.toLowerCase).take(2))
      },
      sql = """
        SELECT LOWER(buyer0.name) as res
        FROM buyer buyer0
        UNION ALL
        SELECT subquery0.res as res
        FROM (SELECT
            LOWER(product0.kebab_case_name) as res
          FROM product product0
          LIMIT 2) subquery0
      """,
      value = Seq("james bond", "叉烧包", "li haoyi", "face-mask", "guitar")
    )


    test("exceptAggregate") - checker(
      query = Text {
        Product.select
          .map(p => (p.name.toLowerCase, p.price))
          // `p.name.toLowerCase` and  `p.kebabCaseName.toLowerCase` are not eliminated, because
          // they are important to the semantics of EXCEPT (and other non-UNION-ALL operators)
          .except(Product.select.map(p => (p.kebabCaseName.toLowerCase, p.price)))
          .aggregate(ps => (ps.maxBy(_._2), ps.minBy(_._2)))
      },
      sql =
        """
        SELECT
          MAX(subquery0.res__1) as res__0,
          MIN(subquery0.res__1) as res__1
        FROM (SELECT
            LOWER(product0.name) as res__0,
            product0.price as res__1
          FROM product product0
          EXCEPT
          SELECT
            LOWER(product0.kebab_case_name) as res__0,
            product0.price as res__1
          FROM product product0) subquery0
      """,
      value = (123.45, 8.88)
    )

    test("unionAllAggregate") - checker(
      query = Text {
        Product.select
          .map(p => (p.name.toLowerCase, p.price))
          // `p.name.toLowerCase` and  `p.kebabCaseName.toLowerCase` get eliminated,
          // as they are not selected by the enclosing query, and cannot affect the UNION ALL
          .unionAll(Product.select.map(p => (p.kebabCaseName.toLowerCase, p.price)))
          .aggregate(ps => (ps.maxBy(_._2), ps.minBy(_._2)))
      },
      sql =
        """
        SELECT
          MAX(subquery0.res__1) as res__0,
          MIN(subquery0.res__1) as res__1
        FROM (SELECT product0.price as res__1
          FROM product product0
          UNION ALL
          SELECT product0.price as res__1
          FROM product product0) subquery0
      """,
      value = (1000.0, 0.1)
    )

    test("deeplyNested") - checker(
      query = Text{
        Buyer.select.map{buyer =>
          buyer.name ->
          ShippingInfo.select
            .filter(_.buyerId === buyer.id)
            .map{shippingInfo =>
              Purchase.select
                .filter(_.shippingInfoId === shippingInfo.id)
                .map{purchase =>
                  Product.select
                    .filter(_.id === purchase.productId)
                    .map(_.price)
                    .sortBy(identity).desc
                    .take(1)
                    .exprQuery
                }
                .sortBy(identity).desc
                .take(1)
                .exprQuery
            }
            .sortBy(identity).desc
            .take(1)
            .exprQuery
        }
      },
      sql = """
      SELECT
        buyer0.name as res__0,
        (SELECT
          (SELECT
            (SELECT product0.price as res
            FROM product product0
            WHERE product0.id = purchase0.product_id
            ORDER BY res DESC
            LIMIT 1) as res
          FROM purchase purchase0
          WHERE purchase0.shipping_info_id = shipping_info0.id
          ORDER BY res DESC
          LIMIT 1) as res
        FROM shipping_info shipping_info0
        WHERE shipping_info0.buyer_id = buyer0.id
        ORDER BY res DESC
        LIMIT 1) as res__1
      FROM buyer buyer0
      """,
      value = Seq(
        ("James Bond", 1000.0),
        ("叉烧包", 300.0),
        ("Li Haoyi", 0.0)
      ),
      docs = """
        Subqueries can be arbitrarily nested. This example traverses four tables
        to find the price of the most expensive product bought by each Buyer, but
        instead of using `JOIN`s it uses subqueries nested 4 layers deep. While this
        example is contrived, it demonstrates how nested ScalaSql `.select` calls
        translate directly into nested SQL subqueries.

        To turn the ScalaSql `Select[T]` into an `Expr[T]`, you can either use
        an aggregate method like `.sumBy(...): Expr[Int]` that generates a `SUM(...)`
        aggregate, or via the `.exprQuery` method that leaves the subquery untouched.
        SQL requires that subqueries used as expressions must return a single row
        and single column, and if the query returns some other number of rows/columns
        most databases will throw an exception, though some like Sqlite will pick
        the first row/column arbitrarily.
      """
    )
  }
}
