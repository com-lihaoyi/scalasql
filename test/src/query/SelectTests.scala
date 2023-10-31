package scalasql.query

import scalasql._
import utest._
import utils.ScalaSqlSuite

import java.time.LocalDate

/**
 * Tests for basic query operations: map, filter, join, etc.
 */
trait SelectTests extends ScalaSqlSuite {
  def tests = Tests {
    test("constant") - checker(query = Expr(1), sql = "SELECT ? as res", value = 1)

    test("table") - checker(
      query = Buyer.select,
      sql = """
        SELECT
          buyer0.id as res__id,
          buyer0.name as res__name,
          buyer0.date_of_birth as res__date_of_birth
        FROM buyer buyer0
      """,
      value = Seq(
        Buyer[Id](id = 1, name = "James Bond", dateOfBirth = LocalDate.parse("2001-02-03")),
        Buyer[Id](id = 2, name = "叉烧包", dateOfBirth = LocalDate.parse("1923-11-12")),
        Buyer[Id](id = 3, name = "Li Haoyi", dateOfBirth = LocalDate.parse("1965-08-09"))
      )
    )

    test("filter") {
      test("single") - checker(
        query = ShippingInfo.select.filter(_.buyerId `=` 2),
        sql = """
        SELECT
          shipping_info0.id as res__id,
          shipping_info0.buyer_id as res__buyer_id,
          shipping_info0.shipping_date as res__shipping_date
        FROM shipping_info shipping_info0
        WHERE shipping_info0.buyer_id = ?
        """,
        value = Seq(
          ShippingInfo[Id](1, 2, LocalDate.parse("2010-02-03")),
          ShippingInfo[Id](3, 2, LocalDate.parse("2012-05-06"))
        )
      )

      test("multiple") - checker(
        query = ShippingInfo.select.filter(_.buyerId `=` 2)
          .filter(_.shippingDate `=` LocalDate.parse("2012-05-06")),
        sql = """
        SELECT
          shipping_info0.id as res__id,
          shipping_info0.buyer_id as res__buyer_id,
          shipping_info0.shipping_date as res__shipping_date
        FROM shipping_info shipping_info0
        WHERE shipping_info0.buyer_id = ?
        AND shipping_info0.shipping_date = ?
      """,
        value =
          Seq(ShippingInfo[Id](id = 3, buyerId = 2, shippingDate = LocalDate.parse("2012-05-06")))
      )

      test("dotSingle") {
        test("pass") - checker(
          query = ShippingInfo.select.filter(_.buyerId `=` 2)
            .filter(_.shippingDate `=` LocalDate.parse("2012-05-06")).single,
          sql = """
            SELECT
              shipping_info0.id as res__id,
              shipping_info0.buyer_id as res__buyer_id,
              shipping_info0.shipping_date as res__shipping_date
            FROM shipping_info shipping_info0
            WHERE shipping_info0.buyer_id = ?
            AND shipping_info0.shipping_date = ?
          """,
          value =
            ShippingInfo[Id](id = 3, buyerId = 2, shippingDate = LocalDate.parse("2012-05-06"))
        )
        test("failTooMany") - intercept[java.lang.AssertionError] {
          checker(
            query = ShippingInfo.select.filter(_.buyerId `=` 2).single,
            value = null.asInstanceOf[ShippingInfo[Id]]
          )
        }
        test("failNotEnough") - intercept[java.lang.AssertionError] {
          checker(
            query = ShippingInfo.select.filter(_.buyerId `=` 123).single,
            value = null.asInstanceOf[ShippingInfo[Id]]
          )
        }
      }

      test("combined") - checker(
        query = ShippingInfo.select
          .filter(p => p.buyerId `=` 2 && p.shippingDate `=` LocalDate.parse("2012-05-06")),
        sql = """
          SELECT
            shipping_info0.id as res__id,
            shipping_info0.buyer_id as res__buyer_id,
            shipping_info0.shipping_date as res__shipping_date
          FROM shipping_info shipping_info0
          WHERE shipping_info0.buyer_id = ?
          AND shipping_info0.shipping_date = ?
        """,
        value = Seq(ShippingInfo[Id](3, 2, LocalDate.parse("2012-05-06")))
      )
    }

    test("map") {
      test("single") - checker(
        query = Buyer.select.map(_.name),
        sql = "SELECT buyer0.name as res FROM buyer buyer0",
        value = Seq("James Bond", "叉烧包", "Li Haoyi")
      )

      test("tuple2") - checker(
        query = Buyer.select.map(c => (c.name, c.id)),
        sql = "SELECT buyer0.name as res__0, buyer0.id as res__1 FROM buyer buyer0",
        value = Seq(("James Bond", 1), ("叉烧包", 2), ("Li Haoyi", 3))
      )

      test("tuple3") - checker(
        query = Buyer.select.map(c => (c.name, c.id, c.dateOfBirth)),
        sql = """
          SELECT
            buyer0.name as res__0,
            buyer0.id as res__1,
            buyer0.date_of_birth as res__2
          FROM buyer buyer0
        """,
        value = Seq(
          ("James Bond", 1, LocalDate.parse("2001-02-03")),
          ("叉烧包", 2, LocalDate.parse("1923-11-12")),
          ("Li Haoyi", 3, LocalDate.parse("1965-08-09"))
        )
      )

      test("interpolateInMap") - checker(
        query = Product.select.map(_.price * 2),
        sql = "SELECT product0.price * ? as res FROM product product0",
        value = Seq(17.76, 600, 6.28, 246.9, 2000.0, 0.2)
      )

      test("heterogenousTuple") - checker(
        query = Buyer.select.map(c => (c.id, c)),
        sql = """
          SELECT
            buyer0.id as res__0,
            buyer0.id as res__1__id,
            buyer0.name as res__1__name,
            buyer0.date_of_birth as res__1__date_of_birth
          FROM buyer buyer0
        """,
        value = Seq(
          (1, Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03"))),
          (2, Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12"))),
          (3, Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09")))
        )
      )
    }

    test("subquery") - checker(
      query = Buyer.select.subquery.map(_.name),
      sql = """
        SELECT subquery0.res__name as res
        FROM (SELECT
            buyer0.id as res__id,
            buyer0.name as res__name,
            buyer0.date_of_birth as res__date_of_birth
          FROM buyer buyer0) subquery0
      """,
      value = Seq("James Bond", "叉烧包", "Li Haoyi")
    )

    test("filterMap") - checker(
      query = Product.select.filter(_.price < 100).map(_.name),
      sql = "SELECT product0.name as res FROM product product0 WHERE product0.price < ?",
      value = Seq("Face Mask", "Socks", "Cookie")
    )

    test("aggregate") {
      test("single") - checker(
        query = Purchase.select.aggregate(_.sumBy(_.total)),
        sql = "SELECT SUM(purchase0.total) as res FROM purchase purchase0",
        value = 12343.2
      )

      test("multiple") - checker(
        query = Purchase.select.aggregate(q => (q.sumBy(_.total), q.maxBy(_.total))),
        sql =
          "SELECT SUM(purchase0.total) as res__0, MAX(purchase0.total) as res__1 FROM purchase purchase0",
        value = (12343.2, 10000.0)
      )
    }

    test("groupBy") - {
      test("simple") - checker(
        query = Purchase.select.groupBy(_.productId)(_.sumBy(_.total)),
        sql = """
          SELECT purchase0.product_id as res__0, SUM(purchase0.total) as res__1
          FROM purchase purchase0
          GROUP BY purchase0.product_id
        """,
        value = Seq((1, 932.4), (2, 900.0), (3, 15.7), (4, 493.8), (5, 10000.0), (6, 1.30)),
        normalize = (x: Seq[(Int, Double)]) => x.sorted
      )

      test("having") - checker(
        query = Purchase.select.groupBy(_.productId)(_.sumBy(_.total)).filter(_._2 > 100)
          .filter(_._1 > 1),
        sql = """
          SELECT purchase0.product_id as res__0, SUM(purchase0.total) as res__1
          FROM purchase purchase0
          GROUP BY purchase0.product_id
          HAVING SUM(purchase0.total) > ? AND purchase0.product_id > ?
        """,
        value = Seq((2, 900.0), (4, 493.8), (5, 10000.0)),
        normalize = (x: Seq[(Int, Double)]) => x.sorted
      )

      test("filterHaving") - checker(
        query = Purchase.select.filter(_.count > 5).groupBy(_.productId)(_.sumBy(_.total))
          .filter(_._2 > 100),
        sql = """
          SELECT purchase0.product_id as res__0, SUM(purchase0.total) as res__1
          FROM purchase purchase0
          WHERE purchase0.count > ?
          GROUP BY purchase0.product_id
          HAVING SUM(purchase0.total) > ?
        """,
        value = Seq((1, 888.0), (5, 10000.0)),
        normalize = (x: Seq[(Int, Double)]) => x.sorted
      )
    }

    test("joins") {
      test("joinFilter") - checker(
        query = Buyer.select.joinOn(ShippingInfo)(_.id `=` _.buyerId).filter(_._1.name `=` "叉烧包"),
        sql = """
          SELECT
            buyer0.id as res__0__id,
            buyer0.name as res__0__name,
            buyer0.date_of_birth as res__0__date_of_birth,
            shipping_info1.id as res__1__id,
            shipping_info1.buyer_id as res__1__buyer_id,
            shipping_info1.shipping_date as res__1__shipping_date
          FROM buyer buyer0
          JOIN shipping_info shipping_info1 ON buyer0.id = shipping_info1.buyer_id
          WHERE buyer0.name = ?
        """,
        value = Seq(
          (
            Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
            ShippingInfo[Id](1, 2, LocalDate.parse("2010-02-03"))
          ),
          (
            Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
            ShippingInfo[Id](3, 2, LocalDate.parse("2012-05-06"))
          )
        )
      )

      test("joinSelectFilter") - checker(
        query = Buyer.select.joinOn(ShippingInfo)(_.id `=` _.buyerId).filter(_._1.name `=` "叉烧包"),
        sql = """
          SELECT
            buyer0.id as res__0__id,
            buyer0.name as res__0__name,
            buyer0.date_of_birth as res__0__date_of_birth,
            shipping_info1.id as res__1__id,
            shipping_info1.buyer_id as res__1__buyer_id,
            shipping_info1.shipping_date as res__1__shipping_date
          FROM buyer buyer0
          JOIN shipping_info shipping_info1 ON buyer0.id = shipping_info1.buyer_id
          WHERE buyer0.name = ?
        """,
        value = Seq(
          (
            Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
            ShippingInfo[Id](1, 2, LocalDate.parse("2010-02-03"))
          ),
          (
            Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
            ShippingInfo[Id](3, 2, LocalDate.parse("2012-05-06"))
          )
        )
      )

      test("joinFilterMap") - checker(
        query = Buyer.select.joinOn(ShippingInfo)(_.id `=` _.buyerId)
          .filter(_._1.name `=` "James Bond").map(_._2.shippingDate),
        sql = """
          SELECT shipping_info1.shipping_date as res
          FROM buyer buyer0
          JOIN shipping_info shipping_info1 ON buyer0.id = shipping_info1.buyer_id
          WHERE buyer0.name = ?
        """,
        value = Seq(LocalDate.parse("2012-04-05"))
      )

      test("selfJoin") - checker(
        query = Buyer.select.joinOn(Buyer)(_.id `=` _.id),
        sql = """
          SELECT
            buyer0.id as res__0__id,
            buyer0.name as res__0__name,
            buyer0.date_of_birth as res__0__date_of_birth,
            buyer1.id as res__1__id,
            buyer1.name as res__1__name,
            buyer1.date_of_birth as res__1__date_of_birth
          FROM buyer buyer0
          JOIN buyer buyer1 ON buyer0.id = buyer1.id
        """,
        value = Seq(
          (
            Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")),
            Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03"))
          ),
          (
            Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
            Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12"))
          ),
          (
            Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09")),
            Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09"))
          )
        )
      )

      test("selfJoin2") - checker(
        query = Buyer.select.joinOn(Buyer)(_.id <> _.id),
        sql = """
          SELECT
            buyer0.id as res__0__id,
            buyer0.name as res__0__name,
            buyer0.date_of_birth as res__0__date_of_birth,
            buyer1.id as res__1__id,
            buyer1.name as res__1__name,
            buyer1.date_of_birth as res__1__date_of_birth
          FROM buyer buyer0
          JOIN buyer buyer1 ON buyer0.id <> buyer1.id
        """,
        value = Seq(
          (
            Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")),
            Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12"))
          ),
          (
            Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")),
            Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09"))
          ),
          (
            Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
            Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03"))
          ),
          (
            Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
            Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09"))
          ),
          (
            Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09")),
            Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03"))
          ),
          (
            Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09")),
            Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12"))
          )
        ),
        normalize = (x: Seq[(Buyer[Id], Buyer[Id])]) => x.sortBy(t => (t._1.id, t._2.id))
      )

      test("flatMap") - checker(
        query = Buyer.select.flatMap(c => ShippingInfo.select.map((c, _))).filter { case (c, p) =>
          c.id `=` p.buyerId && c.name `=` "James Bond"
        }.map(_._2.shippingDate),
        sql = """
          SELECT shipping_info1.shipping_date as res
          FROM buyer buyer0, shipping_info shipping_info1
          WHERE buyer0.id = shipping_info1.buyer_id
          AND buyer0.name = ?
        """,
        value = Seq(LocalDate.parse("2012-04-05"))
      )

      test("flatMap2") - checker(
        query = Buyer.select.flatMap(c =>
          ShippingInfo.select.filter { p => c.id `=` p.buyerId && c.name `=` "James Bond" }
        ).map(_.shippingDate),
        sql = """
          SELECT shipping_info1.shipping_date as res
          FROM buyer buyer0, shipping_info shipping_info1
          WHERE buyer0.id = shipping_info1.buyer_id
          AND buyer0.name = ?
        """,
        value = Seq(LocalDate.parse("2012-04-05"))
      )
    }

    test("distinct") {
      test("nondistinct") - checker(
        query = Purchase.select.map(_.shippingInfoId),
        sql = "SELECT purchase0.shipping_info_id as res FROM purchase purchase0",
        value = Seq(1, 1, 1, 2, 2, 3, 3)
      )

      test("distinct") - checker(
        query = Purchase.select.map(_.shippingInfoId).distinct,
        sql = "SELECT DISTINCT purchase0.shipping_info_id as res FROM purchase purchase0",
        value = Seq(1, 2, 3),
        normalize = (x: Seq[Int]) => x.sorted
      )
    }

    test("contains") - checker(
      query = Buyer.select.filter(b => ShippingInfo.select.map(_.buyerId).contains(b.id)),
      sql = """
        SELECT buyer0.id as res__id, buyer0.name as res__name, buyer0.date_of_birth as res__date_of_birth
        FROM buyer buyer0
        WHERE buyer0.id in (SELECT shipping_info0.buyer_id as res FROM shipping_info shipping_info0)
      """,
      value = Seq(
        Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")),
        Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12"))
      )
    )

    test("nonEmpty") - checker(
      query = Buyer.select
        .map(b => (b.name, ShippingInfo.select.filter(_.buyerId `=` b.id).map(_.id).nonEmpty)),
      sql = """
        SELECT
          buyer0.name as res__0,
          EXISTS (SELECT
            shipping_info0.id as res
            FROM shipping_info shipping_info0
            WHERE shipping_info0.buyer_id = buyer0.id) as res__1
        FROM buyer buyer0
      """,
      value = Seq(("James Bond", true), ("叉烧包", true), ("Li Haoyi", false))
    )

    test("isEmpty") - checker(
      query = Buyer.select
        .map(b => (b.name, ShippingInfo.select.filter(_.buyerId `=` b.id).map(_.id).isEmpty)),
      sql = """
        SELECT
          buyer0.name as res__0,
          NOT EXISTS (SELECT
            shipping_info0.id as res
            FROM shipping_info shipping_info0
            WHERE shipping_info0.buyer_id = buyer0.id) as res__1
        FROM buyer buyer0
      """,
      value = Seq(("James Bond", false), ("叉烧包", false), ("Li Haoyi", true))
    )

    test("case") {
      test("when") - checker(
        query = Product.select.map(p =>
          caseWhen(
            (p.price > 200) -> (p.name + " EXPENSIVE"),
            (p.price > 5) -> (p.name + " NORMAL"),
            (p.price <= 5) -> (p.name + " CHEAP")
          )
        ),
        sqls = Seq(
          """
            SELECT
              CASE
                WHEN product0.price > ? THEN product0.name || ?
                WHEN product0.price > ? THEN product0.name || ?
                WHEN product0.price <= ? THEN product0.name || ?
              END as res
            FROM product product0
          """,
          """
            SELECT
              CASE
                WHEN product0.price > ? THEN CONCAT(product0.name, ?)
                WHEN product0.price > ? THEN CONCAT(product0.name, ?)
                WHEN product0.price <= ? THEN CONCAT(product0.name, ?)
              END as res
            FROM product product0
          """
        ),
        value = Seq(
          "Face Mask NORMAL",
          "Guitar EXPENSIVE",
          "Socks CHEAP",
          "Skate Board NORMAL",
          "Camera EXPENSIVE",
          "Cookie CHEAP"
        )
      )
      test("else") - checker(
        query = Product.select.map(p =>
          caseWhen(
            (p.price > 200) -> (p.name + " EXPENSIVE"),
            (p.price > 5) -> (p.name + " NORMAL")
          ).`else` { p.name + " UNKNOWN" }
        ),
        sqls = Seq(
          """
            SELECT
              CASE
                WHEN product0.price > ? THEN product0.name || ?
                WHEN product0.price > ? THEN product0.name || ?
                ELSE product0.name || ?
              END as res
            FROM product product0
          """,
          """
            SELECT
              CASE
                WHEN product0.price > ? THEN CONCAT(product0.name, ?)
                WHEN product0.price > ? THEN CONCAT(product0.name, ?)
                ELSE CONCAT(product0.name, ?)
              END as res
            FROM product product0
          """
        ),
        value = Seq(
          "Face Mask NORMAL",
          "Guitar EXPENSIVE",
          "Socks UNKNOWN",
          "Skate Board NORMAL",
          "Camera EXPENSIVE",
          "Cookie UNKNOWN"
        )
      )
    }

  }
}
