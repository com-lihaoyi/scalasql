package usql.query

import usql._
import utest._

import java.sql.Date


/**
 * Tests for basic query operations: map, filter, join, etc.
 */
trait SelectTests extends UsqlTestSuite {
  def tests = Tests {
    test("constant") - checker(
      query = Expr(1),
      sql = "SELECT ? as res",
      value = 1
    )

    test("table") - checker(
      query = Buyer.select,
      sql = """
        SELECT
          buyer0.id as res__id,
          buyer0.name as res__name,
          buyer0.date_of_birth as res__date_of_birth
        FROM buyer buyer0
      """,
      value = Seq[Buyer[Val]](
        Buyer(id = 1, name = "James Bond", dateOfBirth = Date.valueOf("2001-02-03")),
        Buyer(id = 2, name = "叉烧包", dateOfBirth = Date.valueOf("1923-11-12")),
        Buyer(id = 3, name = "Li Haoyi", dateOfBirth = Date.valueOf("1965-08-09"))
      )
    )

    test("filter") {
      test("single") - checker(
        query = ShippingInfo.select.filter(_.buyerId === 2),
        sql = """
        SELECT
          shipping_info0.id as res__id,
          shipping_info0.buyer_id as res__buyer_id,
          shipping_info0.shipping_date as res__shipping_date
        FROM shipping_info shipping_info0
        WHERE shipping_info0.buyer_id = ?
        """,
        value = Seq[ShippingInfo[Val]](
          ShippingInfo(1, 2, Date.valueOf("2010-02-03")),
          ShippingInfo(3, 2, Date.valueOf("2012-05-06"))
        )
      )

      test("multiple") - checker(
        query =
          ShippingInfo.select.filter(_.buyerId === 2).filter(_.shippingDate === Date.valueOf("2012-05-06")),
        sql = """
        SELECT
          shipping_info0.id as res__id,
          shipping_info0.buyer_id as res__buyer_id,
          shipping_info0.shipping_date as res__shipping_date
        FROM shipping_info shipping_info0
        WHERE shipping_info0.buyer_id = ?
        AND shipping_info0.shipping_date = ?
      """,
        value = Seq(
          ShippingInfo[Val](id = 3, buyerId = 2, shippingDate = Date.valueOf("2012-05-06"))
        )
      )

      test("combined") - checker(
        query =
          ShippingInfo.select.filter(p => p.buyerId === 2 && p.shippingDate === Date.valueOf("2012-05-06")),
        sql = """
          SELECT
            shipping_info0.id as res__id,
            shipping_info0.buyer_id as res__buyer_id,
            shipping_info0.shipping_date as res__shipping_date
          FROM shipping_info shipping_info0
          WHERE shipping_info0.buyer_id = ?
          AND shipping_info0.shipping_date = ?
        """,
        value = Seq(
          ShippingInfo[Val](3, 2, Date.valueOf("2012-05-06"))
        )
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
          ("James Bond", 1, Date.valueOf("2001-02-03")),
          ("叉烧包", 2, Date.valueOf("1923-11-12")),
          ("Li Haoyi", 3, Date.valueOf("1965-08-09"))
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
          (1, Buyer[Val](1, "James Bond", Date.valueOf("2001-02-03"))),
          (2, Buyer[Val](2, "叉烧包", Date.valueOf("1923-11-12"))),
          (3, Buyer[Val](3, "Li Haoyi", Date.valueOf("1965-08-09")))
        )
      )
    }

    test("filterMap") - checker(
      query = Product.select.filter(_.price < 100).map(_.name),
      sql = "SELECT product0.name as res FROM product product0 WHERE product0.price < ?",
      value = Seq("Face Mask", "Socks", "Cookie")
    )

    test("aggregate") {
      test("single") - checker(
        query =
          Purchase.select.aggregate(_.sumBy(_.total)),
        sql = "SELECT SUM(purchase0.total) as res FROM purchase purchase0",
        value = 12343.2
      )

      test("multiple") - checker(
        query =
          Purchase.select.aggregate(q => (q.sumBy(_.total), q.maxBy(_.total))),
        sql =
          "SELECT SUM(purchase0.total) as res__0, MAX(purchase0.total) as res__1 FROM purchase purchase0",
        value = (12343.2, 10000.0)
      )
    }

    test("groupBy") - {
      test("simple") - checker(
        query =
          Purchase.select.groupBy(_.productId)(_.sumBy(_.total)),
        sql = """
          SELECT purchase0.product_id as res__0, SUM(purchase0.total) as res__1
          FROM purchase purchase0
          GROUP BY purchase0.product_id
        """,
        value = Seq((1, 932.4), (2, 900.0), (3, 15.7), (4, 493.8), (5, 10000.0), (6, 1.30)),
        normalize = (x: Seq[(Int, Double)]) => x.sorted
      )

      test("having") - checker(
        query =
          Purchase.select.groupBy(_.productId)(_.sumBy(_.total)).filter(_._2 > 100).filter(
            _._1 > 1
          ),
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
        query =
          Purchase.select.filter(_.count > 5).groupBy(_.productId)(_.sumBy(_.total)).filter(
            _._2 > 100
          ),
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

    test("sort") {
      test("sort") - checker(
        query = Product.select.sortBy(_.price).map(_.name),
        sql = "SELECT product0.name as res FROM product product0 ORDER BY product0.price",
        value = Seq("Cookie", "Socks", "Face Mask", "Skate Board", "Guitar", "Camera")
      )

      test("sortLimit") - checker(
        query = Product.select.sortBy(_.price).map(_.name).take(2),
        sql = "SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 2",
        value = Seq("Cookie", "Socks")
      )

      test("sortLimitTwiceHigher") - checker(
        query = Product.select.sortBy(_.price).map(_.name).take(2).take(3),
        sql = "SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 2",
        value = Seq("Cookie", "Socks")
      )

      test("sortLimitTwiceLower") - checker(
        query = Product.select.sortBy(_.price).map(_.name).take(2).take(1),
        sql = "SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 1",
        value = Seq("Cookie")
      )

      test("sortLimitOffset") - checker(
        query = Product.select.sortBy(_.price).map(_.name).drop(2).take(2),
        sql =
          "SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 2 OFFSET 2",
        value = Seq("Face Mask", "Skate Board")
      )

      test("sortLimitOffsetTwice") - checker(
        query = Product.select.sortBy(_.price).map(_.name).drop(2).drop(2).take(1),
        sql =
          "SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 1 OFFSET 4",
        value = Seq("Guitar")
      )

      test("sortOffsetLimit") - checker(
        query = Product.select.sortBy(_.price).map(_.name).drop(2).take(2),
        sql =
          "SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 2 OFFSET 2",
        value = Seq("Face Mask", "Skate Board")
      )

      test("sortLimitOffset") - checker(
        query = Product.select.sortBy(_.price).map(_.name).take(2).drop(1),
        sql =
          "SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 1 OFFSET 1",
        value = Seq("Socks")
      )
    }

    test("joins") {
      test("joinFilter") - checker(
        query =
          Buyer.select.joinOn(ShippingInfo)(_.id === _.buyerId).filter(_._1.name === "叉烧包"),
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
            Buyer[Val](2, "叉烧包", Date.valueOf("1923-11-12")),
            ShippingInfo[Val](1, 2, Date.valueOf("2010-02-03"))
          ),
          (
            Buyer[Val](2, "叉烧包", Date.valueOf("1923-11-12")),
            ShippingInfo[Val](3, 2, Date.valueOf("2012-05-06"))
          )
        )
      )

      test("joinSelectFilter") - checker(
        query =
          Buyer.select.joinOn(ShippingInfo)(_.id === _.buyerId)
            .filter(_._1.name === "叉烧包"),
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
            Buyer[Val](2, "叉烧包", Date.valueOf("1923-11-12")),
            ShippingInfo[Val](1, 2, Date.valueOf("2010-02-03"))
          ),
          (
            Buyer[Val](2, "叉烧包", Date.valueOf("1923-11-12")),
            ShippingInfo[Val](3, 2, Date.valueOf("2012-05-06"))
          )
        )
      )

      test("joinFilterMap") - checker(
        query =
          Buyer.select.joinOn(ShippingInfo)(_.id === _.buyerId)
            .filter(_._1.name === "James Bond")
            .map(_._2.shippingDate),
        sql = """
          SELECT shipping_info1.shipping_date as res
          FROM buyer buyer0
          JOIN shipping_info shipping_info1 ON buyer0.id = shipping_info1.buyer_id
          WHERE buyer0.name = ?
        """,
        value = Seq(Date.valueOf("2012-04-05"))
      )

      test("selfJoin") - checker(
        query = Buyer.select.joinOn(Buyer)(_.id === _.id),
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
            Buyer[Val](1, "James Bond", Date.valueOf("2001-02-03")),
            Buyer[Val](1, "James Bond", Date.valueOf("2001-02-03"))
            ),
          (
            Buyer[Val](2, "叉烧包", Date.valueOf("1923-11-12")),
            Buyer[Val](2, "叉烧包", Date.valueOf("1923-11-12"))
          ),
          (
            Buyer[Val](3, "Li Haoyi", Date.valueOf("1965-08-09")),
            Buyer[Val](3, "Li Haoyi", Date.valueOf("1965-08-09"))
          )
        )
      )

      test("selfJoin2") - checker(
        query = Buyer.select.joinOn(Buyer)(_.id !== _.id),
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
            Buyer[Val](1, "James Bond", Date.valueOf("2001-02-03")),
            Buyer[Val](2, "叉烧包", Date.valueOf("1923-11-12"))
          ),
          (
            Buyer[Val](1, "James Bond", Date.valueOf("2001-02-03")),
            Buyer[Val](3, "Li Haoyi", Date.valueOf("1965-08-09"))
          ),
          (
            Buyer[Val](2, "叉烧包", Date.valueOf("1923-11-12")),
            Buyer[Val](1, "James Bond", Date.valueOf("2001-02-03"))
          ),
          (
            Buyer[Val](2, "叉烧包", Date.valueOf("1923-11-12")),
            Buyer[Val](3, "Li Haoyi", Date.valueOf("1965-08-09"))
          ),
          (
            Buyer[Val](3, "Li Haoyi", Date.valueOf("1965-08-09")),
            Buyer[Val](1, "James Bond", Date.valueOf("2001-02-03"))
          ),
          (
            Buyer[Val](3, "Li Haoyi", Date.valueOf("1965-08-09")),
            Buyer[Val](2, "叉烧包", Date.valueOf("1923-11-12"))
          )
        ),
        normalize = (x: Seq[(Buyer[Val], Buyer[Val])]) => x.sortBy(t => (t._1.id(), t._2.id()))
      )

      test("flatMap") - checker(
        query =
          Buyer.select.flatMap(c => ShippingInfo.select.map((c, _)))
            .filter { case (c, p) => c.id === p.buyerId && c.name === "James Bond" }
            .map(_._2.shippingDate),
        sql = """
          SELECT shipping_info1.shipping_date as res
          FROM buyer buyer0, shipping_info shipping_info1
          WHERE buyer0.id = shipping_info1.buyer_id
          AND buyer0.name = ?
        """,
        value = Seq(Date.valueOf("2012-04-05"))
      )

      test("flatMap2") - checker(
        query =
          Buyer.select.flatMap(c =>
            ShippingInfo.select
              .filter { p => c.id === p.buyerId && c.name === "James Bond" }
          ).map(_.shippingDate),
        sql = """
          SELECT shipping_info1.shipping_date as res
          FROM buyer buyer0, shipping_info shipping_info1
          WHERE buyer0.id = shipping_info1.buyer_id
          AND buyer0.name = ?
        """,
        value = Seq(Date.valueOf("2012-04-05"))
      )
    }

    test("distinct") {
      test("nondistinct") - checker(
        query =
          Purchase.select.map(_.shippingInfoId),
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
      query =
        Buyer.select.filter(b => ShippingInfo.select.map(_.buyerId).contains(b.id)),
      sql = """
        SELECT buyer0.id as res__id, buyer0.name as res__name, buyer0.date_of_birth as res__date_of_birth
        FROM buyer buyer0
        WHERE buyer0.id in (SELECT shipping_info0.buyer_id as res FROM shipping_info shipping_info0)
      """,
      value = Seq(
        Buyer[Val](1, "James Bond", Date.valueOf("2001-02-03")),
        Buyer[Val](2, "叉烧包", Date.valueOf("1923-11-12"))
      )
    )

    test("nonEmpty") - checker(
      query =
        Buyer.select.map(b =>
          (b.name, ShippingInfo.select.filter(_.buyerId === b.id).map(_.id).nonEmpty)
        ),
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
      query =
        Buyer.select.map(b =>
          (b.name, ShippingInfo.select.filter(_.buyerId === b.id).map(_.id).isEmpty)
        ),
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

    test("compound") {
      test("union") - checker(
        query =
          Product.select.map(_.name.toLowerCase).union(
            Product.select.map(_.kebabCaseName.toLowerCase)
          ),
        sql = """
          SELECT LOWER(product0.name) as res
          FROM product product0
          UNION
          SELECT LOWER(product0.kebab_case_name) as res
          FROM product product0
        """,
        value = Seq(
          "camera",
          "cookie",
          "face mask",
          "face-mask",
          "guitar",
          "skate board",
          "skate-board",
          "socks"
        ),
        normalize = (x: Seq[String]) => x.sorted
      )

      test("unionAll") - checker(
        query =
          Product.select.map(_.name.toLowerCase).unionAll(
            Product.select.map(_.kebabCaseName.toLowerCase)
          ),
        sql = """
          SELECT LOWER(product0.name) as res
          FROM product product0
          UNION ALL
          SELECT LOWER(product0.kebab_case_name) as res
          FROM product product0
        """,
        value = Seq(
          "face mask",
          "guitar",
          "socks",
          "skate board",
          "camera",
          "cookie",
          "face-mask",
          "guitar",
          "socks",
          "skate-board",
          "camera",
          "cookie"
        )
      )

      test("intersect") - checker(
        query =
          Product.select.map(_.name.toLowerCase).intersect(
            Product.select.map(_.kebabCaseName.toLowerCase)
          ),
        sql = """
          SELECT LOWER(product0.name) as res
          FROM product product0
          INTERSECT
          SELECT LOWER(product0.kebab_case_name) as res
          FROM product product0
        """,
        value = Seq("camera", "cookie", "guitar", "socks"),
        normalize = (x: Seq[String]) => x.sorted
      )

      test("except") - checker(
        query =
          Product.select.map(_.name.toLowerCase).except(
            Product.select.map(_.kebabCaseName.toLowerCase)
          ),
        sql = """
          SELECT LOWER(product0.name) as res
          FROM product product0
          EXCEPT
          SELECT LOWER(product0.kebab_case_name) as res
          FROM product product0
        """,
        value = Seq("face mask", "skate board")
      )

      test("unionAllUnionSort") - checker(
        query =
          Product.select.map(_.name.toLowerCase)
            .unionAll(Buyer.select.map(_.name.toLowerCase))
            .union(Product.select.map(_.kebabCaseName.toLowerCase))
            .sortBy(identity),
        sql = """
          SELECT LOWER(product0.name) as res
          FROM product product0
          UNION ALL
          SELECT LOWER(buyer0.name) as res
          FROM buyer buyer0
          UNION
          SELECT LOWER(product0.kebab_case_name) as res
          FROM product product0
          ORDER BY res
        """,
        value = Seq(
          "camera",
          "cookie",
          "face mask",
          "face-mask",
          "guitar",
          "james bond",
          "li haoyi",
          "skate board",
          "skate-board",
          "socks",
          "叉烧包"
        )
      )

      test("unionAllUnionSortLimit") - checker(
        query =
          Product.select.map(_.name.toLowerCase)
            .unionAll(Buyer.select.map(_.name.toLowerCase))
            .union(Product.select.map(_.kebabCaseName.toLowerCase))
            .sortBy(identity)
            .drop(4)
            .take(4),
        sql = """
          SELECT LOWER(product0.name) as res
          FROM product product0
          UNION ALL
          SELECT LOWER(buyer0.name) as res
          FROM buyer buyer0
          UNION
          SELECT LOWER(product0.kebab_case_name) as res
          FROM product product0
          ORDER BY res
          LIMIT 4
          OFFSET 4
        """,
        value = Seq("guitar", "james bond", "li haoyi", "skate board")
      )

      test("intersectAggregate") - checker(
        query =
          Product.select
            .map(p => (p.name.toLowerCase, p.price))
            .except(Product.select.map(p => (p.kebabCaseName.toLowerCase, p.price)))
            .aggregate(ps => (ps.maxBy(_._2), ps.minBy(_._2))),
        sql = """
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
    }
  }
}
