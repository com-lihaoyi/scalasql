package usql.query

import usql._
import usql.query.Expr
import utest._
import ExprOps._

/**
 * Tests for basic query operations: map, filter, join, etc.
 */
object SelectTests extends TestSuite {
  val checker = new TestDb("querytests")
  def tests = Tests {
    test("constant") - checker(Expr(1)).expect(
      sql = "SELECT ? as res",
      value = 1
    )

    test("table") - checker(Buyer.select).expect(
      sql = """
        SELECT
          buyer0.id as res__id,
          buyer0.name as res__name,
          buyer0.date_of_birth as res__date_of_birth
        FROM buyer buyer0
      """,
      value = Vector(
        Buyer(id = 1, name = "James Bond", dateOfBirth = "2001-02-03"),
        Buyer(id = 2, name = "叉烧包", dateOfBirth = "1923-11-12"),
        Buyer(id = 3, name = "Li Haoyi", dateOfBirth = "1965-08-09")
      )
    )

    test("filter"){
      test("single") - checker(ShippingInfo.select.filter(_.buyerId === 2)).expect(
        sql = """
        SELECT
          shipping_info0.id as res__id,
          shipping_info0.buyer_id as res__buyer_id,
          shipping_info0.shipping_date as res__shipping_date
        FROM shipping_info shipping_info0
        WHERE shipping_info0.buyer_id = ?
        """,
        value = Vector(
          ShippingInfo(id = 1, buyerId = 2, shippingDate = "2010-02-03"),
          ShippingInfo(id = 3, buyerId = 2, shippingDate = "2012-05-06")
        )
      )

      test("multiple") - checker(
        ShippingInfo.select.filter(_.buyerId === 2).filter(_.shippingDate === "2012-05-06")
      ).expect(
        sql = """
        SELECT
          shipping_info0.id as res__id,
          shipping_info0.buyer_id as res__buyer_id,
          shipping_info0.shipping_date as res__shipping_date
        FROM shipping_info shipping_info0
        WHERE shipping_info0.buyer_id = ?
        AND shipping_info0.shipping_date = ?
      """,
        value = Vector(
          ShippingInfo(id = 3, buyerId = 2, shippingDate = "2012-05-06")
        )
      )
      test("combined") - checker(
        ShippingInfo.select.filter(p => p.buyerId === 2 && p.shippingDate === "2012-05-06")
      ).expect(
        sql = """
          SELECT
            shipping_info0.id as res__id,
            shipping_info0.buyer_id as res__buyer_id,
            shipping_info0.shipping_date as res__shipping_date
          FROM shipping_info shipping_info0
          WHERE shipping_info0.buyer_id = ?
          AND shipping_info0.shipping_date = ?
        """,
        value = Vector(
          ShippingInfo(id = 3, buyerId = 2, shippingDate = "2012-05-06")
        )
      )
    }

    test("map"){
      test("single") - checker(Buyer.select.map(_.name)).expect(
        sql = "SELECT buyer0.name as res FROM buyer buyer0",
        value = Vector("James Bond", "叉烧包", "Li Haoyi")
      )

      test("tuple2") - checker(Buyer.select.map(c => (c.name, c.id))).expect(
        sql = "SELECT buyer0.name as res__0, buyer0.id as res__1 FROM buyer buyer0",
        value =  Vector(("James Bond", 1), ("叉烧包", 2), ("Li Haoyi", 3))
      )

      test("tuple3") - checker(Buyer.select.map(c => (c.name, c.id, c.dateOfBirth))).expect(
        sql = """
          SELECT
            buyer0.name as res__0,
            buyer0.id as res__1,
            buyer0.date_of_birth as res__2
          FROM buyer buyer0
        """,
        value =  Vector(
          ("James Bond", 1, "2001-02-03"),
          ("叉烧包", 2, "1923-11-12"),
          ("Li Haoyi", 3, "1965-08-09")
        )
      )

      test("interpolateInMap") - checker(Product.select.map(_.price * 2)).expect(
        sql = "SELECT product0.price * ? as res FROM product product0",
        value = Vector(17.76, 600, 6.28, 246.9, 2000.0, 0.2)
      )

      test("heterogenousTuple") - checker(Buyer.select.map(c => (c.id, c))).expect(
        sql = """
          SELECT
            buyer0.id as res__0,
            buyer0.id as res__1__id,
            buyer0.name as res__1__name,
            buyer0.date_of_birth as res__1__date_of_birth
          FROM buyer buyer0
        """,
        value = Vector(
          (1, Buyer(id = 1, name = "James Bond", dateOfBirth = "2001-02-03")),
          (2, Buyer(id = 2, name = "叉烧包", dateOfBirth = "1923-11-12")),
          (3, Buyer(id = 3, name = "Li Haoyi", dateOfBirth = "1965-08-09"))
        )
      )
    }

    test("filterMap") - checker(Product.select.filter(_.price < 100).map(_.name)).expect(
      sql = "SELECT product0.name as res FROM product product0 WHERE product0.price < ?",
      value = Vector("Face Mask", "Socks", "Cookie")
    )

    test("aggregate"){
      test("single") - checker(
        Purchase.select.aggregate(_.sumBy(_.total))
      ).expect(
        sql = "SELECT SUM(purchase0.total) as res FROM purchase purchase0",
        value = 12343.2
      )
      test("multiple") - checker(
        Purchase.select.aggregate(q => (q.sumBy(_.total), q.maxBy(_.total)))
      ).expect(
        sql = "SELECT SUM(purchase0.total) as res__0, MAX(purchase0.total) as res__1 FROM purchase purchase0",
        value = (12343.2, 10000.0)
      )
    }

    test("groupBy") - {
      test("simple") - checker(
        Purchase.select.groupBy(_.productId)(_.sumBy(_.total))
      ).expect(
        sql = """
          SELECT purchase0.product_id as res__0, SUM(purchase0.total) as res__1
          FROM purchase purchase0
          GROUP BY purchase0.product_id
        """,
        value = Vector((1, 932.4), (2, 900.0), (3, 15.7), (4, 493.8), (5, 10000.0), (6, 1.30))

      )

      test("having") - checker(
        Purchase.select.groupBy(_.productId)(_.sumBy(_.total)).filter(_._2 > 100).filter(_._1 > 1)
      ).expect(
        sql = """
          SELECT purchase0.product_id as res__0, SUM(purchase0.total) as res__1
          FROM purchase purchase0
          GROUP BY purchase0.product_id
          HAVING SUM(purchase0.total) > ? AND purchase0.product_id > ?
        """,
        value = Vector((2, 900.0), (4, 493.8), (5, 10000.0))
      )

      test("filterHaving") - checker(
        Purchase.select.filter(_.count > 5).groupBy(_.productId)(_.sumBy(_.total)).filter(_._2 > 100)
      ).expect(
        sql = """
          SELECT purchase0.product_id as res__0, SUM(purchase0.total) as res__1
          FROM purchase purchase0
          WHERE purchase0.count > ?
          GROUP BY purchase0.product_id
          HAVING SUM(purchase0.total) > ?
        """,
        value = Vector((1, 888.0), (5, 10000.0))
      )
    }

    test("sort") {
      test("sort") - checker(Product.select.sortBy(_.price).map(_.name)).expect(
        sql = "SELECT product0.name as res FROM product product0 ORDER BY product0.price",
        value = Vector("Cookie", "Socks", "Face Mask", "Skate Board", "Guitar", "Camera")
      )

      test("sortLimit") - checker(Product.select.sortBy(_.price).map(_.name).take(2)).expect(
        sql = "SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 2",
        value = Vector("Cookie", "Socks")
      )

      test("sortLimitTwiceHigher") - checker(Product.select.sortBy(_.price).map(_.name).take(2).take(3)).expect(
        sql = "SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 2",
        value = Vector("Cookie", "Socks")
      )

      test("sortLimitTwiceLower") - checker(Product.select.sortBy(_.price).map(_.name).take(2).take(1)).expect(
        sql = "SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 1",
        value = Vector("Cookie")
      )

      test("sortLimitOffset") - checker(Product.select.sortBy(_.price).map(_.name).drop(2).take(2)).expect(
        sql = "SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 2 OFFSET 2",
        value = Vector("Face Mask", "Skate Board")
      )

      test("sortLimitOffsetTwice") - checker(Product.select.sortBy(_.price).map(_.name).drop(2).drop(2).take(1)).expect(
        sql = "SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 1 OFFSET 4",
        value = Vector("Guitar")
      )

      test("sortOffsetLimit") - checker(Product.select.sortBy(_.price).map(_.name).drop(2).take(2)).expect(
        sql = "SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 2 OFFSET 2",
        value = Vector("Face Mask", "Skate Board")
      )

      test("sortLimitOffset") - checker(Product.select.sortBy(_.price).map(_.name).take(2).drop(1)).expect(
        sql = "SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 1 OFFSET 1",
        value = Vector("Socks")
      )
    }

    test("joins"){
      test("joinFilter") - checker(
        Buyer.select.joinOn(ShippingInfo)(_.id === _.buyerId).filter(_._1.name === "叉烧包")
      ).expect(
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
        value = Vector(
          (
            Buyer(id = 2, name = "叉烧包", dateOfBirth = "1923-11-12"),
            ShippingInfo(id = 1, buyerId = 2, shippingDate = "2010-02-03")
          ),
          (
            Buyer(id = 2, name = "叉烧包", dateOfBirth = "1923-11-12"),
            ShippingInfo(id = 3, buyerId = 2, shippingDate = "2012-05-06")
          )
        )
      )

      test("joinSelectFilter") - checker(
        Buyer.select.joinOn(ShippingInfo)(_.id === _.buyerId)
          .filter(_._1.name === "叉烧包")
      ).expect(
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
        value = Vector(
          (
            Buyer(id = 2, name = "叉烧包", dateOfBirth = "1923-11-12"),
            ShippingInfo(id = 1, buyerId = 2, shippingDate = "2010-02-03")
          ),
          (
            Buyer(id = 2, name = "叉烧包", dateOfBirth = "1923-11-12"),
            ShippingInfo(id = 3, buyerId = 2, shippingDate = "2012-05-06")
          )
        )
      )

      test("joinFilterMap") - checker(
        Buyer.select.joinOn(ShippingInfo)(_.id === _.buyerId)
          .filter(_._1.name === "James Bond")
          .map(_._2.shippingDate)
      ).expect(
        sql = """
          SELECT shipping_info1.shipping_date as res
          FROM buyer buyer0
          JOIN shipping_info shipping_info1 ON buyer0.id = shipping_info1.buyer_id
          WHERE buyer0.name = ?
        """,
        value = Vector("2012-04-05")
      )

      test("flatMap") - checker(
        Buyer.select.flatMap(c => ShippingInfo.select.map((c, _)))
          .filter{case (c, p) => c.id === p.buyerId && c.name === "James Bond"}
          .map(_._2.shippingDate)
      ).expect(
        sql = """
          SELECT shipping_info1.shipping_date as res
          FROM buyer buyer0, shipping_info shipping_info1
          WHERE buyer0.id = shipping_info1.buyer_id
          AND buyer0.name = ?
        """,
        value = Vector("2012-04-05")
      )
      test("flatMap2") - checker(
        Buyer.select.flatMap(c =>
          ShippingInfo.select
            .filter { p => c.id === p.buyerId && c.name === "James Bond" }
        ).map(_.shippingDate)

      ).expect(
        sql = """
          SELECT shipping_info1.shipping_date as res
          FROM buyer buyer0, shipping_info shipping_info1
          WHERE buyer0.id = shipping_info1.buyer_id
          AND buyer0.name = ?
        """,
        value = Vector("2012-04-05")
      )
    }

    test("distinct"){
      test("nondistinct") - checker(
        Purchase.select.map(_.shippingInfoId)
      ).expect(
        sql = "SELECT purchase0.shipping_info_id as res FROM purchase purchase0",
        value = Vector(1, 1, 1, 2, 2, 3, 3)
      )

      test("distinct") - checker(Purchase.select.map(_.shippingInfoId).distinct).expect(
        sql = "SELECT DISTINCT purchase0.shipping_info_id as res FROM purchase purchase0",
        value = Vector(1, 2, 3)
      )
    }


    test("contains") - checker(
      Buyer.select.filter(b => ShippingInfo.select.map(_.buyerId).contains(b.id))
    ).expect(
      sql = """
        SELECT buyer0.id as res__id, buyer0.name as res__name, buyer0.date_of_birth as res__date_of_birth
        FROM buyer buyer0
        WHERE buyer0.id in (SELECT shipping_info0.buyer_id as res FROM shipping_info shipping_info0)
      """,
      value = Vector(
        Buyer(1, "James Bond", "2001-02-03"),
        Buyer(2, "叉烧包", "1923-11-12")
      )
    )

    test("nonEmpty") - checker(
      Buyer.select.map(b => (b.name, ShippingInfo.select.filter(_.buyerId === b.id).map(_.id).nonEmpty))
    ).expect(
      sql = """
        SELECT
          buyer0.name as res__0,
          EXISTS (SELECT
            shipping_info0.id as res
            FROM shipping_info shipping_info0
            WHERE shipping_info0.buyer_id = buyer0.id) as res__1
        FROM buyer buyer0
      """,
      value = Vector(("James Bond", true), ("叉烧包", true), ("Li Haoyi", false))
    )

    test("isEmpty") - checker(
      Buyer.select.map(b => (b.name, ShippingInfo.select.filter(_.buyerId === b.id).map(_.id).isEmpty))
    ).expect(
      sql = """
        SELECT
          buyer0.name as res__0,
          NOT EXISTS (SELECT
            shipping_info0.id as res
            FROM shipping_info shipping_info0
            WHERE shipping_info0.buyer_id = buyer0.id) as res__1
        FROM buyer buyer0
      """,
      value = Vector(("James Bond", false), ("叉烧包", false), ("Li Haoyi", true))
    )

    test("compound") {
      test("union") - checker(
        Product.select.map(_.name.toLowerCase).union(Product.select.map(_.kebabCaseName.toLowerCase))
      ).expect(
        sql = """
          SELECT LOWER(product0.name) as res
          FROM product product0
          UNION
          SELECT LOWER(product0.kebab_case_name) as res
          FROM product product0
        """,
        value = Vector(
          "camera",
          "cookie",
          "face mask",
          "face-mask",
          "guitar",
          "skate board",
          "skate-board",
          "socks"
        )
      )

      test("unionAll") - checker(
        Product.select.map(_.name.toLowerCase).unionAll(Product.select.map(_.kebabCaseName.toLowerCase))
      ).expect(
        sql = """
          SELECT LOWER(product0.name) as res
          FROM product product0
          UNION ALL
          SELECT LOWER(product0.kebab_case_name) as res
          FROM product product0
        """,
        value = Vector(
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
        Product.select.map(_.name.toLowerCase).intersect(Product.select.map(_.kebabCaseName.toLowerCase))
      ).expect(
        sql = """
          SELECT LOWER(product0.name) as res
          FROM product product0
          INTERSECT
          SELECT LOWER(product0.kebab_case_name) as res
          FROM product product0
        """,
        value = Vector("camera", "cookie", "guitar", "socks")
      )

      test("except") - checker(
        Product.select.map(_.name.toLowerCase).except(Product.select.map(_.kebabCaseName.toLowerCase))
      ).expect(
        sql = """
          SELECT LOWER(product0.name) as res
          FROM product product0
          EXCEPT
          SELECT LOWER(product0.kebab_case_name) as res
          FROM product product0
        """,
        value = Vector("face mask", "skate board")
      )

      test("unionAllUnionSort") - checker(
        Product.select.map(_.name.toLowerCase)
          .unionAll(Buyer.select.map(_.name.toLowerCase))
          .union(Product.select.map(_.kebabCaseName.toLowerCase))
          .sortBy(identity)
      ).expect(
        sql = """
          SELECT LOWER(product0.name) as res
          FROM product product0
          UNION ALL
          SELECT LOWER(buyer0.name) as res
          FROM buyer buyer0
          UNION
          SELECT LOWER(product0.kebab_case_name) as res
          FROM product product0
          ORDER BY LOWER(product0.name)
        """,
        value = Vector(
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
        Product.select.map(_.name.toLowerCase)
          .unionAll(Buyer.select.map(_.name.toLowerCase))
          .union(Product.select.map(_.kebabCaseName.toLowerCase))
          .sortBy(identity)
          .drop(4)
          .take(4)
      ).expect(
        sql = """
          SELECT LOWER(product0.name) as res
          FROM product product0
          UNION ALL
          SELECT LOWER(buyer0.name) as res
          FROM buyer buyer0
          UNION
          SELECT LOWER(product0.kebab_case_name) as res
          FROM product product0
          ORDER BY LOWER(product0.name)
          LIMIT 4
          OFFSET 4
        """,
        value = Vector("guitar", "james bond", "li haoyi", "skate board")
      )

      test("intersectAggregate") - checker(
        Product.select
          .map(p => (p.name.toLowerCase, p.price))
          .except(Product.select.map(p => (p.kebabCaseName.toLowerCase, p.price)))
          .aggregate(ps => (ps.maxBy(_._2), ps.minBy(_._2)))
      ).expect(
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
