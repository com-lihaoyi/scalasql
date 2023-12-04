package scalasql.query

import scalasql._
import sourcecode.Text
import utest._
import utils.ScalaSqlSuite

import java.time.LocalDate

trait FlatJoinTests extends ScalaSqlSuite {
  def description = "inner `JOIN`s, `JOIN ON`s, self-joins, `LEFT`/`RIGHT`/`OUTER` `JOIN`s"
  def tests = Tests {
    test("join") - checker(
      query = Text {
        for {
          b <- Buyer.select
          si <- ShippingInfo.join(_.buyerId `=` b.id)
        } yield (b.name, si.shippingDate)
      },
      sql = """
        SELECT buyer0.name AS res_0, shipping_info1.shipping_date AS res_1
        FROM buyer buyer0
        JOIN shipping_info shipping_info1 ON (shipping_info1.buyer_id = buyer0.id)
      """,
      value = Seq(
        ("James Bond", LocalDate.parse("2012-04-05")),
        ("叉烧包", LocalDate.parse("2010-02-03")),
        ("叉烧包", LocalDate.parse("2012-05-06"))
      ),
      docs = """
        "flat" joins using `for`-comprehensions are allowed. These allow you to
        "flatten out" the nested tuples you get from normal `.join` clauses,
        letting you write natural looking queries without deeply nested tuples.
      """,
      normalize = (x: Seq[(String, LocalDate)]) => x.sortBy(t => (t._1, t._2.toEpochDay))
    )
    test("join3") - checker(
      query = Text {
        for {
          b <- Buyer.select
          if b.name === "Li Haoyi"
          si <- ShippingInfo.join(_.id `=` b.id)
          pu <- Purchase.join(_.shippingInfoId `=` si.id)
          pr <- Product.join(_.id `=` pu.productId)
          if pr.price > 1.0
        } yield (b.name, pr.name, pr.price)
      },
      sql = """
        SELECT buyer0.name AS res_0, product3.name AS res_1, product3.price AS res_2
        FROM buyer buyer0
        JOIN shipping_info shipping_info1 ON (shipping_info1.id = buyer0.id)
        JOIN purchase purchase2 ON (purchase2.shipping_info_id = shipping_info1.id)
        JOIN product product3 ON (product3.id = purchase2.product_id)
        WHERE (buyer0.name = ?) AND (product3.price > ?)
      """,
      value = Seq(
        ("Li Haoyi", "Face Mask", 8.88)
      ),
      docs = """
        "flat" joins using `for`-comprehensions can have multiple `.join` clauses that
        translate to SQL `JOIN ON`s, as well as `if` clauses that translate to SQL
        `WHERE` clauses. This example uses multiple flat `.join`s together with `if`
        clauses to query the products purchased by the user `"Li Haoyi"` that have
        a price more than `1.0` dollars
      """
    )

    test("leftJoin") - checker(
      query = Text {
        for {
          b <- Buyer.select
          si <- ShippingInfo.leftJoin(_.buyerId `=` b.id)
        } yield (b.name, si.map(_.shippingDate))
      },
      sql = """
        SELECT buyer0.name AS res_0, shipping_info1.shipping_date AS res_1
        FROM buyer buyer0
        LEFT JOIN shipping_info shipping_info1 ON (shipping_info1.buyer_id = buyer0.id)
      """,
      value = Seq(
        ("James Bond", Some(LocalDate.parse("2012-04-05"))),
        ("Li Haoyi", None),
        ("叉烧包", Some(LocalDate.parse("2010-02-03"))),
        ("叉烧包", Some(LocalDate.parse("2012-05-06")))
      ),
      docs = """
        Flat joins can also support `.leftJoin`s, where the table being joined
        is given to you as a `JoinNullable[T]`
      """,
      normalize =
        (x: Seq[(String, Option[LocalDate])]) => x.sortBy(t => (t._1, t._2.map(_.toEpochDay)))
    )
    test("flatMap") - checker(
      query = Text {
        Buyer.select
          .flatMap(b => ShippingInfo.crossJoin().map((b, _)))
          .filter { case (b, s) => b.id `=` s.buyerId && b.name `=` "James Bond" }
          .map(_._2.shippingDate)
      },
      sql = """
        SELECT shipping_info1.shipping_date AS res
        FROM buyer buyer0
        CROSS JOIN shipping_info shipping_info1
        WHERE ((buyer0.id = shipping_info1.buyer_id) AND (buyer0.name = ?))
      """,
      value = Seq(LocalDate.parse("2012-04-05")),
      docs = """
        You can also perform inner joins via `flatMap`, either by directly
        calling `.flatMap` or via `for`-comprehensions as below. This can help
        reduce the boilerplate when dealing with lots of joins.
      """
    )

    test("flatMapFor") - checker(
      query = Text {
        for {
          b <- Buyer.select
          s <- ShippingInfo.crossJoin()
          if b.id `=` s.buyerId && b.name `=` "James Bond"
        } yield s.shippingDate
      },
      sql = """
        SELECT shipping_info1.shipping_date AS res
        FROM buyer buyer0
        CROSS JOIN shipping_info shipping_info1
        WHERE ((buyer0.id = shipping_info1.buyer_id) AND (buyer0.name = ?))
      """,
      value = Seq(LocalDate.parse("2012-04-05")),
      docs = """
        You can also perform inner joins via `flatMap
      """
    )

    test("flatMapForFilter") - checker(
      query = Text {
        for {
          b <- Buyer.select.filter(_.name `=` "James Bond")
          s <- ShippingInfo.crossJoin().filter(b.id `=` _.buyerId)
        } yield s.shippingDate
      },
      sql = """
        SELECT shipping_info1.shipping_date AS res
        FROM buyer buyer0
        CROSS JOIN shipping_info shipping_info1
        WHERE (buyer0.name = ?) AND (buyer0.id = shipping_info1.buyer_id)
      """,
      value = Seq(LocalDate.parse("2012-04-05"))
    )

    test("flatMapForJoin") - checker(
      query = Text {
        for {
          (b, si) <- Buyer.select.join(ShippingInfo)(_.id `=` _.buyerId)
          (pu, pr) <- Purchase.select.join(Product)(_.productId `=` _.id).crossJoin()
          if si.id `=` pu.shippingInfoId
        } yield (b.name, pr.name)
      },
      sql = """
        SELECT buyer0.name AS res_0, subquery2.res_1_name AS res_1
        FROM buyer buyer0
        JOIN shipping_info shipping_info1 ON (buyer0.id = shipping_info1.buyer_id)
        CROSS JOIN (SELECT
            purchase2.shipping_info_id AS res_0_shipping_info_id,
            product3.name AS res_1_name
          FROM purchase purchase2
          JOIN product product3 ON (purchase2.product_id = product3.id)) subquery2
        WHERE (shipping_info1.id = subquery2.res_0_shipping_info_id)
      """,
      value = Seq(
        ("James Bond", "Camera"),
        ("James Bond", "Skate Board"),
        ("叉烧包", "Cookie"),
        ("叉烧包", "Face Mask"),
        ("叉烧包", "Face Mask"),
        ("叉烧包", "Guitar"),
        ("叉烧包", "Socks")
      ),
      docs = """
        Using queries with `join`s in a `for`-comprehension is supported, with the
        generated `JOIN`s being added to the `FROM` clause generated by the `.flatMap`.
      """,
      normalize = (x: Seq[(String, String)]) => x.sorted
    )

    test("flatMapForGroupBy") - checker(
      query = Text {
        for {
          (name, dateOfBirth) <- Buyer.select.groupBy(_.name)(_.minBy(_.dateOfBirth))
          shippingInfo <- ShippingInfo.crossJoin()
        } yield (name, dateOfBirth, shippingInfo.id, shippingInfo.shippingDate)
      },
      sql = """
        SELECT
          subquery0.res_0 AS res_0,
          subquery0.res_1 AS res_1,
          shipping_info1.id AS res_2,
          shipping_info1.shipping_date AS res_3
        FROM (SELECT buyer0.name AS res_0, MIN(buyer0.date_of_birth) AS res_1
          FROM buyer buyer0
          GROUP BY buyer0.name) subquery0
        CROSS JOIN shipping_info shipping_info1
      """,
      value = Seq(
        ("James Bond", LocalDate.parse("2001-02-03"), 1, LocalDate.parse("2010-02-03")),
        ("James Bond", LocalDate.parse("2001-02-03"), 2, LocalDate.parse("2012-04-05")),
        ("James Bond", LocalDate.parse("2001-02-03"), 3, LocalDate.parse("2012-05-06")),
        ("Li Haoyi", LocalDate.parse("1965-08-09"), 1, LocalDate.parse("2010-02-03")),
        ("Li Haoyi", LocalDate.parse("1965-08-09"), 2, LocalDate.parse("2012-04-05")),
        ("Li Haoyi", LocalDate.parse("1965-08-09"), 3, LocalDate.parse("2012-05-06")),
        ("叉烧包", LocalDate.parse("1923-11-12"), 1, LocalDate.parse("2010-02-03")),
        ("叉烧包", LocalDate.parse("1923-11-12"), 2, LocalDate.parse("2012-04-05")),
        ("叉烧包", LocalDate.parse("1923-11-12"), 3, LocalDate.parse("2012-05-06"))
      ),
      docs = """
        Using non-trivial queries in the `for`-comprehension may result in subqueries
        being generated
      """,
      normalize = (x: Seq[(String, LocalDate, Int, LocalDate)]) => x.sortBy(t => (t._1, t._3))
    )
    test("flatMapForGroupBy2") - checker(
      query = Text {
        for {
          (name, dateOfBirth) <- Buyer.select.groupBy(_.name)(_.minBy(_.dateOfBirth))
          (shippingInfoId, shippingDate) <- ShippingInfo.select
            .groupBy(_.id)(_.minBy(_.shippingDate))
            .crossJoin()
        } yield (name, dateOfBirth, shippingInfoId, shippingDate)
      },
      sql = """
        SELECT
          subquery0.res_0 AS res_0,
          subquery0.res_1 AS res_1,
          subquery1.res_0 AS res_2,
          subquery1.res_1 AS res_3
        FROM (SELECT
            buyer0.name AS res_0,
            MIN(buyer0.date_of_birth) AS res_1
          FROM buyer buyer0
          GROUP BY buyer0.name) subquery0
        CROSS JOIN (SELECT
            shipping_info1.id AS res_0,
            MIN(shipping_info1.shipping_date) AS res_1
          FROM shipping_info shipping_info1
          GROUP BY shipping_info1.id) subquery1
      """,
      value = Seq(
        ("James Bond", LocalDate.parse("2001-02-03"), 1, LocalDate.parse("2010-02-03")),
        ("James Bond", LocalDate.parse("2001-02-03"), 2, LocalDate.parse("2012-04-05")),
        ("James Bond", LocalDate.parse("2001-02-03"), 3, LocalDate.parse("2012-05-06")),
        ("Li Haoyi", LocalDate.parse("1965-08-09"), 1, LocalDate.parse("2010-02-03")),
        ("Li Haoyi", LocalDate.parse("1965-08-09"), 2, LocalDate.parse("2012-04-05")),
        ("Li Haoyi", LocalDate.parse("1965-08-09"), 3, LocalDate.parse("2012-05-06")),
        ("叉烧包", LocalDate.parse("1923-11-12"), 1, LocalDate.parse("2010-02-03")),
        ("叉烧包", LocalDate.parse("1923-11-12"), 2, LocalDate.parse("2012-04-05")),
        ("叉烧包", LocalDate.parse("1923-11-12"), 3, LocalDate.parse("2012-05-06"))
      ),
      docs = """
        Using non-trivial queries in the `for`-comprehension may result in subqueries
        being generated
      """,
      normalize = (x: Seq[(String, LocalDate, Int, LocalDate)]) => x.sortBy(t => (t._1, t._3))
    )
    test("flatMapForCompound") - checker(
      query = Text {
        for {
          b <- Buyer.select.sortBy(_.id).asc.take(1)
          si <- ShippingInfo.select.sortBy(_.id).asc.take(1).crossJoin()
        } yield (b.name, si.shippingDate)
      },
      sql = """
        SELECT
          subquery0.name AS res_0,
          subquery1.shipping_date AS res_1
        FROM
          (SELECT buyer0.id AS id, buyer0.name AS name
          FROM buyer buyer0
          ORDER BY id ASC
          LIMIT ?) subquery0
        CROSS JOIN (SELECT
            shipping_info1.id AS id,
            shipping_info1.shipping_date AS shipping_date
          FROM shipping_info shipping_info1
          ORDER BY id ASC
          LIMIT ?) subquery1
      """,
      value = Seq(
        ("James Bond", LocalDate.parse("2010-02-03"))
      ),
      docs = """
        Using non-trivial queries in the `for`-comprehension may result in subqueries
        being generated
      """
    )

  }
}
