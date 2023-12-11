package scalasql.query

import scalasql._
import sourcecode.Text
import utest._
import utils.ScalaSqlSuite

trait CompoundSelectTests extends ScalaSqlSuite {
  def description = "Compound `SELECT` operations: sort, take, drop, union, unionAll, etc."

  def tests = Tests {

    test("sort") {
      test("simple") - checker(
        query = Text { Product.select.sortBy(_.price).map(_.name) },
        sql = "SELECT product0.name AS res FROM product product0 ORDER BY product0.price",
        value = Seq("Cookie", "Socks", "Face Mask", "Skate Board", "Guitar", "Camera"),
        docs = """
          ScalaSql's `.sortBy` method translates into SQL `ORDER BY`
        """
      )

      test("twice") - checker(
        query = Text { Purchase.select.sortBy(_.productId).asc.sortBy(_.shippingInfoId).desc },
        sql = """
          SELECT
            purchase0.id AS id,
            purchase0.shipping_info_id AS shipping_info_id,
            purchase0.product_id AS product_id,
            purchase0.count AS count,
            purchase0.total AS total
          FROM purchase purchase0
          ORDER BY shipping_info_id DESC, product_id ASC""",
        value = Seq(
          Purchase[Sc](6, 3, 1, 5, 44.4),
          Purchase[Sc](7, 3, 6, 13, 1.3),
          Purchase[Sc](4, 2, 4, 4, 493.8),
          Purchase[Sc](5, 2, 5, 10, 10000.0),
          Purchase[Sc](1, 1, 1, 100, 888.0),
          Purchase[Sc](2, 1, 2, 3, 900.0),
          Purchase[Sc](3, 1, 3, 5, 15.7)
        ),
        docs = """
          If you want to sort by multiple columns, you can call `.sortBy` multiple times,
          each with its own call to `.asc` or `.desc`. Note that the rightmost call to `.sortBy`
          takes precedence, following the Scala collections `.sortBy` semantics, and so the
          right-most `.sortBy` in ScalaSql becomes the _left_-most entry in the SQL `ORDER BY` clause
        """
      )

      test("sortLimit") - checker(
        query = Text { Product.select.sortBy(_.price).map(_.name).take(2) },
        sql = "SELECT product0.name AS res FROM product product0 ORDER BY product0.price LIMIT ?",
        value = Seq("Cookie", "Socks"),
        docs = """
          ScalaSql also supports various combinations of `.take` and `.drop`, translating to SQL
          `LIMIT` or `OFFSET`
        """
      )
      test("sortOffset") - checker(
        query = Text { Product.select.sortBy(_.price).map(_.name).drop(2) },
        sqls = Seq(
          "SELECT product0.name AS res FROM product product0 ORDER BY product0.price OFFSET ?",
          "SELECT product0.name AS res FROM product product0 ORDER BY product0.price LIMIT ? OFFSET ?"
        ),
        value = Seq("Face Mask", "Skate Board", "Guitar", "Camera")
      )

      test("sortLimitTwiceHigher") - checker(
        query = Text { Product.select.sortBy(_.price).map(_.name).take(2).take(3) },
        sql = "SELECT product0.name AS res FROM product product0 ORDER BY product0.price LIMIT ?",
        value = Seq("Cookie", "Socks"),
        docs = """
          Note that `.drop` and `.take` follow Scala collections' semantics, so calling e.g. `.take`
          multiple times takes the value of the smallest `.take`, while calling `.drop` multiple
          times accumulates the total amount dropped
        """
      )

      test("sortLimitTwiceLower") - checker(
        query = Text { Product.select.sortBy(_.price).map(_.name).take(2).take(1) },
        sql = "SELECT product0.name AS res FROM product product0 ORDER BY product0.price LIMIT ?",
        value = Seq("Cookie")
      )

      test("sortLimitOffset") - checker(
        query = Text { Product.select.sortBy(_.price).map(_.name).drop(2).take(2) },
        sql =
          "SELECT product0.name AS res FROM product product0 ORDER BY product0.price LIMIT ? OFFSET ?",
        value = Seq("Face Mask", "Skate Board")
      )

      test("sortLimitOffsetTwice") - checker(
        query = Text { Product.select.sortBy(_.price).map(_.name).drop(2).drop(2).take(1) },
        sql =
          "SELECT product0.name AS res FROM product product0 ORDER BY product0.price LIMIT ? OFFSET ?",
        value = Seq("Guitar")
      )

      test("sortOffsetLimit") - checker(
        query = Text { Product.select.sortBy(_.price).map(_.name).drop(2).take(2) },
        sql =
          "SELECT product0.name AS res FROM product product0 ORDER BY product0.price LIMIT ? OFFSET ?",
        value = Seq("Face Mask", "Skate Board")
      )

      test("sortLimitOffset") - checker(
        query = Text { Product.select.sortBy(_.price).map(_.name).take(2).drop(1) },
        sql =
          "SELECT product0.name AS res FROM product product0 ORDER BY product0.price LIMIT ? OFFSET ?",
        value = Seq("Socks")
      )
    }

    test("distinct") - checker(
      query = Text { Purchase.select.sortBy(_.total).desc.take(3).map(_.shippingInfoId).distinct },
      sql = """
        SELECT DISTINCT subquery0.res AS res
        FROM (SELECT purchase0.shipping_info_id AS res
          FROM purchase purchase0
          ORDER BY purchase0.total DESC
          LIMIT ?) subquery0
      """,
      value = Seq(1, 2),
      normalize = (x: Seq[Int]) => x.sorted,
      docs = """
        ScalaSql's `.distinct` translates to SQL's `SELECT DISTINCT`
      """
    )

    test("flatMap") - checker(
      query = Text {
        Purchase.select.sortBy(_.total).desc.take(3).flatMap { p =>
          Product.crossJoin().filter(_.id === p.productId).map(_.name)
        }
      },
      sql = """
        SELECT product1.name AS res
        FROM (SELECT purchase0.product_id AS product_id, purchase0.total AS total
          FROM purchase purchase0
          ORDER BY total DESC
          LIMIT ?) subquery0
        CROSS JOIN product product1
        WHERE (product1.id = subquery0.product_id)
      """,
      value = Seq("Camera", "Face Mask", "Guitar"),
      normalize = (x: Seq[String]) => x.sorted,
      docs = """
        Many operations in SQL cannot be done in certain orders, unless you move part of the logic into
        a subquery. ScalaSql does this automatically for you, e.g. doing a `flatMap`, `.sumBy`, or
        `.aggregate` after a `.sortBy`/`.take`, the LHS `.sortBy`/`.take` is automatically extracted
        into a subquery
      """
    )

    test("sumBy") - checker(
      query = Text { Purchase.select.sortBy(_.total).desc.take(3).sumBy(_.total) },
      sql = """
        SELECT SUM(subquery0.total) AS res
        FROM (SELECT purchase0.total AS total
          FROM purchase purchase0
          ORDER BY total DESC
          LIMIT ?) subquery0
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
        SELECT SUM(subquery0.total) AS res_0, AVG(subquery0.total) AS res_1
        FROM (SELECT purchase0.total AS total
          FROM purchase purchase0
          ORDER BY total DESC
          LIMIT ?) subquery0
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
        SELECT LOWER(product0.name) AS res
        FROM product product0
        UNION
        SELECT LOWER(product0.kebab_case_name) AS res
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
      normalize = (x: Seq[String]) => x.sorted,
      docs = """
        ScalaSql's `.union`/`.unionAll`/`.intersect`/`.except` translate into SQL's
        `UNION`/`UNION ALL`/`INTERSECT`/`EXCEPT`.
      """
    )

    test("unionAll") - checker(
      query = Text {
        Product.select
          .map(_.name.toLowerCase)
          .unionAll(Product.select.map(_.kebabCaseName.toLowerCase))
      },
      sql = """
        SELECT LOWER(product0.name) AS res
        FROM product product0
        UNION ALL
        SELECT LOWER(product0.kebab_case_name) AS res
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
        SELECT LOWER(product0.name) AS res
        FROM product product0
        INTERSECT
        SELECT LOWER(product0.kebab_case_name) AS res
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
        SELECT LOWER(product0.name) AS res
        FROM product product0
        EXCEPT
        SELECT LOWER(product0.kebab_case_name) AS res
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
        SELECT LOWER(product0.name) AS res
        FROM product product0
        UNION ALL
        SELECT LOWER(buyer0.name) AS res
        FROM buyer buyer0
        UNION
        SELECT LOWER(product0.kebab_case_name) AS res
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
      ),
      docs = """
        Performing a `.sortBy` after `.union` or `.unionAll` applies the sort
        to both sides of the `union`/`unionAll`, behaving identically to Scala or SQL
      """
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
        SELECT LOWER(product0.name) AS res
        FROM product product0
        UNION ALL
        SELECT LOWER(buyer0.name) AS res
        FROM buyer buyer0
        UNION
        SELECT LOWER(product0.kebab_case_name) AS res
        FROM product product0
        ORDER BY res
        LIMIT ?
        OFFSET ?
      """,
      value = Seq("guitar", "james bond", "li haoyi", "skate board")
    )
  }
}
