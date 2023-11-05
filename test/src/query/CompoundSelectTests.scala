package scalasql.query

import scalasql._
import sourcecode.Text
import utest._
import utils.ScalaSqlSuite

import java.time.LocalDate

/**
 * Tests for basic query operations: map, filter, join, etc.
 */
trait CompoundSelectTests extends ScalaSqlSuite {
  def tests = Tests {

    test("sort") {
      test("simple") - checker(
        query = Text { Product.select.sortBy(_.price).map(_.name) },
        sql = "SELECT product0.name as res FROM product product0 ORDER BY product0.price",
        value = Seq("Cookie", "Socks", "Face Mask", "Skate Board", "Guitar", "Camera")
      )
      test("twice") - checker(
        query = Text { Purchase.select.sortBy(_.productId).asc.sortBy(_.shippingInfoId).desc },
        sql = """
          SELECT
            purchase0.id as res__id,
            purchase0.shipping_info_id as res__shipping_info_id,
            purchase0.product_id as res__product_id,
            purchase0.count as res__count,
            purchase0.total as res__total
          FROM purchase purchase0
          ORDER BY res__shipping_info_id DESC, res__product_id ASC""",
        value = Seq(
          Purchase[Id](6, 3, 1, 5, 44.4),
          Purchase[Id](7, 3, 6, 13, 1.3),
          Purchase[Id](4, 2, 4, 4, 493.8),
          Purchase[Id](5, 2, 5, 10, 10000.0),
          Purchase[Id](1, 1, 1, 100, 888.0),
          Purchase[Id](2, 1, 2, 3, 900.0),
          Purchase[Id](3, 1, 3, 5, 15.7)
        )
      )

      test("sortLimit") - checker(
        query = Text { Product.select.sortBy(_.price).map(_.name).take(2) },
        sql = "SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 2",
        value = Seq("Cookie", "Socks")
      )
      test("sortOffset") - checker(
        query = Text { Product.select.sortBy(_.price).map(_.name).drop(2) },
        sqls = Seq(
          "SELECT product0.name as res FROM product product0 ORDER BY product0.price OFFSET 2",
          "SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 2147483647 OFFSET 2"
        ),
        value = Seq("Face Mask", "Skate Board", "Guitar", "Camera")
      )

      test("sortLimitTwiceHigher") - checker(
        query = Text { Product.select.sortBy(_.price).map(_.name).take(2).take(3) },
        sql = "SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 2",
        value = Seq("Cookie", "Socks")
      )

      test("sortLimitTwiceLower") - checker(
        query = Text { Product.select.sortBy(_.price).map(_.name).take(2).take(1) },
        sql = "SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 1",
        value = Seq("Cookie")
      )

      test("sortLimitOffset") - checker(
        query = Text { Product.select.sortBy(_.price).map(_.name).drop(2).take(2) },
        sql =
          "SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 2 OFFSET 2",
        value = Seq("Face Mask", "Skate Board")
      )

      test("sortLimitOffsetTwice") - checker(
        query = Text { Product.select.sortBy(_.price).map(_.name).drop(2).drop(2).take(1) },
        sql =
          "SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 1 OFFSET 4",
        value = Seq("Guitar")
      )

      test("sortOffsetLimit") - checker(
        query = Text { Product.select.sortBy(_.price).map(_.name).drop(2).take(2) },
        sql =
          "SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 2 OFFSET 2",
        value = Seq("Face Mask", "Skate Board")
      )

      test("sortLimitOffset") - checker(
        query = Text { Product.select.sortBy(_.price).map(_.name).take(2).drop(1) },
        sql =
          "SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 1 OFFSET 1",
        value = Seq("Socks")
      )
    }

    test("distinct") - checker(
      query = Text { Purchase.select.sortBy(_.total).desc.take(3).map(_.shippingInfoId).distinct },
      sql = """
        SELECT DISTINCT subquery0.res as res
        FROM (SELECT purchase0.shipping_info_id as res
          FROM purchase purchase0
          ORDER BY purchase0.total DESC
          LIMIT 3) subquery0
      """,
      value = Seq(1, 2),
      normalize = (x: Seq[Int]) => x.sorted
    )

    test("flatMap") - checker(
      query = Text {
        Purchase.select.sortBy(_.total).desc.take(3).flatMap { p =>
          Product.select.filter(_.id === p.productId).map(_.name)
        }
      },
      sql = """
        SELECT product1.name as res
        FROM (SELECT purchase0.product_id as res__product_id, purchase0.total as res__total
          FROM purchase purchase0
          ORDER BY res__total DESC
          LIMIT 3) subquery0, product product1
        WHERE product1.id = subquery0.res__product_id
      """,
      value = Seq("Camera", "Face Mask", "Guitar"),
      normalize = (x: Seq[String]) => x.sorted
    )

    test("sumBy") - checker(
      query = Text { Purchase.select.sortBy(_.total).desc.take(3).sumBy(_.total) },
      sql = """
        SELECT SUM(subquery0.res__total) as res
        FROM (SELECT purchase0.total as res__total
          FROM purchase purchase0
          ORDER BY res__total DESC
          LIMIT 3) subquery0
      """,
      value = 11788.0,
      normalize = (x: Double) => x.round.toDouble
    )

    test("aggregate") - checker(
      query = Text {
        Purchase.select
          .sortBy(_.total)
          .desc
          .take(3)
          .aggregate(p => (p.sumBy(_.total), p.avgBy(_.total)))
      },
      sql = """
        SELECT SUM(subquery0.res__total) as res__0, AVG(subquery0.res__total) as res__1
        FROM (SELECT purchase0.total as res__total
          FROM purchase purchase0
          ORDER BY res__total DESC
          LIMIT 3) subquery0
      """,
      value = (11788.0, 3929.0),
      normalize = (x: (Double, Double)) => (x._1.round.toDouble, x._2.round.toDouble)
    )

    test("union") - checker(
      query = Text {
        Product.select
          .map(_.name.toLowerCase)
          .union(Product.select.map(_.kebabCaseName.toLowerCase))
      },
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
      query = Text {
        Product.select
          .map(_.name.toLowerCase)
          .unionAll(Product.select.map(_.kebabCaseName.toLowerCase))
      },
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
      query = Text {
        Product.select
          .map(_.name.toLowerCase)
          .intersect(Product.select.map(_.kebabCaseName.toLowerCase))
      },
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
      query = Text {
        Product.select
          .map(_.name.toLowerCase)
          .except(Product.select.map(_.kebabCaseName.toLowerCase))
      },
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
      query = Text {
        Product.select
          .map(_.name.toLowerCase)
          .unionAll(Buyer.select.map(_.name.toLowerCase))
          .union(Product.select.map(_.kebabCaseName.toLowerCase))
          .sortBy(identity)
      },
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
      query = Text {
        Product.select
          .map(_.name.toLowerCase)
          .unionAll(Buyer.select.map(_.name.toLowerCase))
          .union(Product.select.map(_.kebabCaseName.toLowerCase))
          .sortBy(identity)
          .drop(4)
          .take(4)
      },
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

    test("exceptAggregate") - checker(
      query = Text {
        Product.select
          .map(p => (p.name.toLowerCase, p.price))
          // `p.name.toLowerCase` and  `p.kebabCaseName.toLowerCase` are not eliminated, because
          // they are important to the semantics of EXCEPT (and other non-UNION-ALL operators)
          .except(Product.select.map(p => (p.kebabCaseName.toLowerCase, p.price)))
          .aggregate(ps => (ps.maxBy(_._2), ps.minBy(_._2)))
      },
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

    test("unionAllAggregate") - checker(
      query = Text {
        Product.select
          .map(p => (p.name.toLowerCase, p.price))
          // `p.name.toLowerCase` and  `p.kebabCaseName.toLowerCase` get eliminated,
          // as they are not selected by the enclosing query, and cannot affect the UNION ALL
          .unionAll(Product.select.map(p => (p.kebabCaseName.toLowerCase, p.price)))
          .aggregate(ps => (ps.maxBy(_._2), ps.minBy(_._2)))
      },
      sql = """
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

  }
}
