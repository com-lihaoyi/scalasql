package scalasql.query

import scalasql._
import sourcecode.Text
import utest._
import utils.ScalaSqlSuite

import java.time.LocalDate

trait SelectTests extends ScalaSqlSuite {
  def description = "Basic `SELECT` operations: map, filter, join, etc."

  def tests = Tests {
    test("constant") - checker(
      query = Text { Expr(1) + Expr(2) },
      sql = "SELECT (? + ?) AS res",
      value = 3,
      docs = """
        The most simple thing you can query in the database is an `Expr`. These do not need
        to be related to any database tables, and translate into raw `SELECT` calls without
        `FROM`.
      """
    )

    test("table") - checker(
      query = Text { Buyer.select },
      sql = """
        SELECT buyer0.id AS id, buyer0.name AS name, buyer0.date_of_birth AS date_of_birth
        FROM buyer buyer0
      """,
      value = Seq(
        Buyer[Sc](id = 1, name = "James Bond", dateOfBirth = LocalDate.parse("2001-02-03")),
        Buyer[Sc](id = 2, name = "叉烧包", dateOfBirth = LocalDate.parse("1923-11-12")),
        Buyer[Sc](id = 3, name = "Li Haoyi", dateOfBirth = LocalDate.parse("1965-08-09"))
      ),
      docs = """
        You can list the contents of a table via the query `Table.select`. It returns a
        `Seq[CaseClass[Sc]]` with the entire contents of the table. Note that listing
        entire tables can be prohibitively expensive on real-world databases, and you
        should generally use `filter`s as shown below
      """
    )

    test("filter") {
      test("single") - checker(
        query = Text { ShippingInfo.select.filter(_.buyerId `=` 2) },
        sql = """
          SELECT
              shipping_info0.id AS id,
              shipping_info0.buyer_id AS buyer_id,
              shipping_info0.shipping_date AS shipping_date
            FROM shipping_info shipping_info0
            WHERE (shipping_info0.buyer_id = ?)
        """,
        value = Seq(
          ShippingInfo[Sc](1, 2, LocalDate.parse("2010-02-03")),
          ShippingInfo[Sc](3, 2, LocalDate.parse("2012-05-06"))
        ),
        docs = """
          ScalaSql's `.filter` translates to SQL `WHERE`, in this case we
          are searching for rows with a particular `buyerId`
        """
      )

      test("multiple") - checker(
        query = Text {
          ShippingInfo.select
            .filter(_.buyerId `=` 2)
            .filter(_.shippingDate `=` LocalDate.parse("2012-05-06"))
        },
        sql = """
          SELECT
            shipping_info0.id AS id,
            shipping_info0.buyer_id AS buyer_id,
            shipping_info0.shipping_date AS shipping_date
          FROM shipping_info shipping_info0
          WHERE (shipping_info0.buyer_id = ?) AND (shipping_info0.shipping_date = ?)
        """,
        value =
          Seq(ShippingInfo[Sc](id = 3, buyerId = 2, shippingDate = LocalDate.parse("2012-05-06"))),
        docs = """
          You can stack multiple `.filter`s on a query.
        """
      )

      test("dotSingle") {
        test("pass") - checker(
          query = Text {
            ShippingInfo.select
              .filter(_.buyerId `=` 2)
              .filter(_.shippingDate `=` LocalDate.parse("2012-05-06"))
              .single
          },
          sql = """
            SELECT
              shipping_info0.id AS id,
              shipping_info0.buyer_id AS buyer_id,
              shipping_info0.shipping_date AS shipping_date
            FROM shipping_info shipping_info0
            WHERE (shipping_info0.buyer_id = ?) AND (shipping_info0.shipping_date = ?)
          """,
          value =
            ShippingInfo[Sc](id = 3, buyerId = 2, shippingDate = LocalDate.parse("2012-05-06")),
          docs = """
            Queries that you expect to return a single row can be annotated with `.single`.
            This changes the return type of the `.select` from `Seq[T]` to just `T`, and throws
            an exception if zero or multiple rows were returned
          """
        )
        test("failTooMany") - intercept[java.lang.AssertionError] {
          checker(
            query = ShippingInfo.select.filter(_.buyerId `=` 2).single,
            value = null.asInstanceOf[ShippingInfo[Sc]]
          )
        }
        test("failNotEnough") - intercept[java.lang.AssertionError] {
          checker(
            query = ShippingInfo.select.filter(_.buyerId `=` 123).single,
            value = null.asInstanceOf[ShippingInfo[Sc]]
          )
        }
      }

      test("combined") - checker(
        query = Text {
          ShippingInfo.select
            .filter(p => p.buyerId `=` 2 && p.shippingDate `=` LocalDate.parse("2012-05-06"))
        },
        sql = """
          SELECT
            shipping_info0.id AS id,
            shipping_info0.buyer_id AS buyer_id,
            shipping_info0.shipping_date AS shipping_date
          FROM shipping_info shipping_info0
          WHERE ((shipping_info0.buyer_id = ?) AND (shipping_info0.shipping_date = ?))
        """,
        value = Seq(ShippingInfo[Sc](3, 2, LocalDate.parse("2012-05-06"))),
        docs = """
          You can perform multiple checks in a single filter using `&&`
        """
      )
    }

    test("map") {
      test("single") - checker(
        query = Text { Buyer.select.map(_.name) },
        sql = "SELECT buyer0.name AS res FROM buyer buyer0",
        value = Seq("James Bond", "叉烧包", "Li Haoyi"),
        docs = """
          `.map` allows you to select exactly what you want to return from
          a query, rather than returning the entire row. Here, we return
          only the `name`s of the `Buyer`s
        """
      )

      test("filterMap") - checker(
        query = Text {
          Product.select.filter(_.price < 100).map(_.name)
        },
        sql = "SELECT product0.name AS res FROM product product0 WHERE (product0.price < ?)",
        value = Seq("Face Mask", "Socks", "Cookie"),
        docs = """
          The common use case of `SELECT FROM WHERE` can be achieved via `.select.filter.map` in ScalaSql
        """
      )

      test("tuple2") - checker(
        query = Text { Buyer.select.map(c => (c.name, c.id)) },
        sql = "SELECT buyer0.name AS res_0, buyer0.id AS res_1 FROM buyer buyer0",
        value = Seq(("James Bond", 1), ("叉烧包", 2), ("Li Haoyi", 3)),
        docs = """
          You can return multiple values from your `.map` by returning a tuple in your query,
          which translates into a `Seq[Tuple]` being returned when the query is run
        """
      )

      test("tuple3") - checker(
        query = Text { Buyer.select.map(c => (c.name, c.id, c.dateOfBirth)) },
        sql = """
          SELECT
            buyer0.name AS res_0,
            buyer0.id AS res_1,
            buyer0.date_of_birth AS res_2
          FROM buyer buyer0
        """,
        value = Seq(
          ("James Bond", 1, LocalDate.parse("2001-02-03")),
          ("叉烧包", 2, LocalDate.parse("1923-11-12")),
          ("Li Haoyi", 3, LocalDate.parse("1965-08-09"))
        )
      )

      test("interpolateInMap") - checker(
        query = Text { Product.select.map(_.price * 2) },
        sql = "SELECT (product0.price * ?) AS res FROM product product0",
        value = Seq(17.76, 600, 6.28, 246.9, 2000.0, 0.2),
        docs = """
          You can perform operations inside the `.map` to change what you return
        """
      )

      test("heterogenousTuple") - checker(
        query = Text { Buyer.select.map(c => (c.id, c)) },
        sql = """
          SELECT
            buyer0.id AS res_0,
            buyer0.id AS res_1_id,
            buyer0.name AS res_1_name,
            buyer0.date_of_birth AS res_1_date_of_birth
          FROM buyer buyer0
        """,
        value = Seq(
          (1, Buyer[Sc](1, "James Bond", LocalDate.parse("2001-02-03"))),
          (2, Buyer[Sc](2, "叉烧包", LocalDate.parse("1923-11-12"))),
          (3, Buyer[Sc](3, "Li Haoyi", LocalDate.parse("1965-08-09")))
        ),
        docs = """
          `.map` can return any combination of tuples, `case class`es, and primitives,
          arbitrarily nested. here we return a tuple of `(Int, Buyer[Sc])`
        """
      )
    }

    test("toExpr") - checker(
      query = Text {
        Product.select.map(p =>
          (
            p.name,
            Purchase.select
              .filter(_.productId === p.id)
              .sortBy(_.total)
              .desc
              .take(1)
              .map(_.total)
              .toExpr
          )
        )
      },
      sql = """
        SELECT
          product0.name AS res_0,
          (SELECT purchase1.total AS res
            FROM purchase purchase1
            WHERE (purchase1.product_id = product0.id)
            ORDER BY res DESC
            LIMIT ?) AS res_1
        FROM product product0""",
      value = Seq(
        ("Face Mask", 888.0),
        ("Guitar", 900.0),
        ("Socks", 15.7),
        ("Skate Board", 493.8),
        ("Camera", 10000.0),
        ("Cookie", 1.3)
      ),
      docs = """
        `SELECT` queries that return a single row and column can be used as SQL expressions
        in standard SQL databases. In ScalaSql, this is done by the `.toExpr` method,
        which turns a `Select[T]` into an `Expr[T]`. Note that if the `Select` returns more
        than one row or column, the database may select a row arbitrarily or will throw
        an exception at runtime (depend on implenmentation)
      """
    )

    test("subquery") - checker(
      query = Text { Buyer.select.subquery.map(_.name) },
      sql = """
        SELECT subquery0.name AS res
        FROM (SELECT buyer0.name AS name FROM buyer buyer0) subquery0
      """,
      value = Seq("James Bond", "叉烧包", "Li Haoyi"),
      docs = """
        ScalaSql generally combines operations like `.map` and `.filter` to minimize the
        number of subqueries to keep the generated SQL readable. If you explicitly want
        a subquery for some reason (e.g. to influence the database query planner), you can
        use the `.subquery` to force a query to be translated into a standalone subquery
      """
    )

    test("aggregate") {
      test("single") - checker(
        query = Text { Purchase.select.sumBy(_.total) },
        sql = "SELECT SUM(purchase0.total) AS res FROM purchase purchase0",
        value = 12343.2,
        docs = """
          You can use methods like `.sumBy` or `.sum` on your queries to generate
          SQL `SUM(...)` aggregates
        """
      )

      test("multiple") - checker(
        query = Text { Purchase.select.aggregate(q => (q.sumBy(_.total), q.maxBy(_.total))) },
        sql =
          "SELECT SUM(purchase0.total) AS res_0, MAX(purchase0.total) AS res_1 FROM purchase purchase0",
        value = (12343.2, 10000.0),
        docs = """
          If you want to perform multiple aggregates at once, you can use the `.aggregate` method
          which takes a function allowing you to call multiple aggregates inside of it
        """
      )
    }

    test("groupBy") - {
      test("simple") - checker(
        query = Text { Purchase.select.groupBy(_.productId)(_.sumBy(_.total)) },
        sql = """
          SELECT purchase0.product_id AS res_0, SUM(purchase0.total) AS res_1
          FROM purchase purchase0
          GROUP BY purchase0.product_id
        """,
        value = Seq((1, 932.4), (2, 900.0), (3, 15.7), (4, 493.8), (5, 10000.0), (6, 1.30)),
        normalize = (x: Seq[(Int, Double)]) => x.sorted,
        docs = """
          ScalaSql's `.groupBy` method translates into a SQL `GROUP BY`. Unlike the normal
          `.groupBy` provided by `scala.Seq`, ScalaSql's `.groupBy` requires you to pass
          an aggregate as a second parameter, mirroring the SQL requirement that any
          column not part of the `GROUP BY` clause has to be in an aggregate.
        """
      )

      test("having") - checker(
        query = Text {
          Purchase.select.groupBy(_.productId)(_.sumBy(_.total)).filter(_._2 > 100).filter(_._1 > 1)
        },
        sql = """
          SELECT purchase0.product_id AS res_0, SUM(purchase0.total) AS res_1
          FROM purchase purchase0
          GROUP BY purchase0.product_id
          HAVING (SUM(purchase0.total) > ?) AND (purchase0.product_id > ?)
        """,
        value = Seq((2, 900.0), (4, 493.8), (5, 10000.0)),
        normalize = (x: Seq[(Int, Double)]) => x.sorted,
        docs = """
          `.filter` calls following a `.groupBy` are automatically translated to SQL `HAVING` clauses
        """
      )

      test("filterHaving") - checker(
        query = Text {
          Purchase.select
            .filter(_.count > 5)
            .groupBy(_.productId)(_.sumBy(_.total))
            .filter(_._2 > 100)
        },
        sql = """
          SELECT purchase0.product_id AS res_0, SUM(purchase0.total) AS res_1
          FROM purchase purchase0
          WHERE (purchase0.count > ?)
          GROUP BY purchase0.product_id
          HAVING (SUM(purchase0.total) > ?)
        """,
        value = Seq((1, 888.0), (5, 10000.0)),
        normalize = (x: Seq[(Int, Double)]) => x.sorted
      )

      test("multipleKeys") - checker(
        query = Text {
          Purchase.select.groupBy(x => (x.shippingInfoId, x.productId))(_.sumBy(_.total))
        },
        sql = """
          SELECT
            purchase0.shipping_info_id AS res_0_0,
            purchase0.product_id AS res_0_1,
            SUM(purchase0.total) AS res_1
          FROM 
            purchase purchase0
          GROUP BY
            purchase0.shipping_info_id,
            purchase0.product_id
        """,
        value = Seq(
          ((1, 1), 888.0),
          ((1, 2), 900.0),
          ((1, 3), 15.7),
          ((2, 4), 493.8),
          ((2, 5), 10000.0),
          ((3, 1), 44.4),
          ((3, 6), 1.3)
        ),
        normalize = (x: Seq[((Int, Int), Double)]) => x.sorted
      )

      test("multipleKeysHaving") - checker(
        query = Text {
          Purchase.select
            .groupBy(x => (x.shippingInfoId, x.productId))(_.sumBy(_.total))
            .filter(_._2 > 10)
            .filter(_._2 < 100)
        },
        sql = """
          SELECT
            purchase0.shipping_info_id AS res_0_0,
            purchase0.product_id AS res_0_1,
            SUM(purchase0.total) AS res_1
          FROM 
            purchase purchase0
          GROUP BY
            purchase0.shipping_info_id,
            purchase0.product_id
          HAVING 
            (SUM(purchase0.total) > ?) AND (SUM(purchase0.total) < ?)
        """,
        value = Seq(((1, 3), 15.7), ((3, 1), 44.4)),
        normalize = (x: Seq[((Int, Int), Double)]) => x.sorted
      )
    }

    test("distinct") {
      test("nondistinct") - checker(
        query = Text { Purchase.select.map(_.shippingInfoId) },
        sql = "SELECT purchase0.shipping_info_id AS res FROM purchase purchase0",
        value = Seq(1, 1, 1, 2, 2, 3, 3),
        docs = """
          Normal queries can allow duplicates in the returned row values, as seen below.
          You can use the `.distinct` operator (translates to SQl's `SELECT DISTINCT`)
          to eliminate those duplicates
        """
      )

      test("distinct") - checker(
        query = Text { Purchase.select.map(_.shippingInfoId).distinct },
        sql = "SELECT DISTINCT purchase0.shipping_info_id AS res FROM purchase purchase0",
        value = Seq(1, 2, 3),
        normalize = (x: Seq[Int]) => x.sorted
      )

      test("subquery") - checker(
        query = Text { ShippingInfo.select.distinct.subquery.map(_.buyerId) },
        sql = """
          SELECT subquery0.buyer_id AS res
          FROM (SELECT DISTINCT
              shipping_info0.id AS id,
              shipping_info0.buyer_id AS buyer_id,
              shipping_info0.shipping_date AS shipping_date
            FROM shipping_info shipping_info0) subquery0
        """,
        value = Seq(1, 2, 2),
        normalize = (x: Seq[Int]) => x.sorted,
        docs = "Columns inside nested subqueries cannot be elided when `SELECT DISTINCT` is used"
      )
    }

    test("contains") - checker(
      query = Text { Buyer.select.filter(b => ShippingInfo.select.map(_.buyerId).contains(b.id)) },
      sql = """
        SELECT buyer0.id AS id, buyer0.name AS name, buyer0.date_of_birth AS date_of_birth
        FROM buyer buyer0
        WHERE (buyer0.id IN (SELECT shipping_info1.buyer_id AS res FROM shipping_info shipping_info1))
      """,
      value = Seq(
        Buyer[Sc](1, "James Bond", LocalDate.parse("2001-02-03")),
        Buyer[Sc](2, "叉烧包", LocalDate.parse("1923-11-12"))
      ),
      docs = """
        ScalaSql's `.contains` method translates into SQL's `IN` syntax, e.g. here checking if a
        subquery contains a column as part of a `WHERE` clause
      """
    )

    test("containsMultiple") - checker(
      query = Text {
        Buyer.select.filter(b =>
          ShippingInfo.select
            .map(s => (s.buyerId, s.shippingDate))
            .contains((b.id, LocalDate.parse("2010-02-03")))
        )
      },
      sql = """
        SELECT buyer0.id AS id, buyer0.name AS name, buyer0.date_of_birth AS date_of_birth
        FROM buyer buyer0
        WHERE ((buyer0.id, ?) IN (SELECT
            shipping_info1.buyer_id AS res_0,
            shipping_info1.shipping_date AS res_1
          FROM shipping_info shipping_info1))
      """,
      value = Seq(
        Buyer[Sc](2, "叉烧包", LocalDate.parse("1923-11-12"))
      ),
      docs = """
      ScalaSql's `.contains` can take a compound Scala value, which translates into
        SQL's `IN` syntax on a tuple with multiple columns. e.g. this query uses that ability
        to find the `Buyer` which has a shipment on a specific date, as an alternative
        to doing a `JOIN`.
      """
    )

    test("nonEmpty") - checker(
      query = Text {
        Buyer.select
          .map(b => (b.name, ShippingInfo.select.filter(_.buyerId `=` b.id).map(_.id).nonEmpty))
      },
      sql = """
        SELECT
          buyer0.name AS res_0,
          (EXISTS (SELECT
            shipping_info1.id AS res
            FROM shipping_info shipping_info1
            WHERE (shipping_info1.buyer_id = buyer0.id))) AS res_1
        FROM buyer buyer0
      """,
      value = Seq(("James Bond", true), ("叉烧包", true), ("Li Haoyi", false)),
      docs = """
        ScalaSql's `.nonEmpty` and `.isEmpty` translates to SQL's `EXISTS` and `NOT EXISTS` syntax
      """
    )

    test("isEmpty") - checker(
      query = Text {
        Buyer.select
          .map(b => (b.name, ShippingInfo.select.filter(_.buyerId `=` b.id).map(_.id).isEmpty))
      },
      sql = """
        SELECT
          buyer0.name AS res_0,
          (NOT EXISTS (SELECT
            shipping_info1.id AS res
            FROM shipping_info shipping_info1
            WHERE (shipping_info1.buyer_id = buyer0.id))) AS res_1
        FROM buyer buyer0
      """,
      value = Seq(("James Bond", false), ("叉烧包", false), ("Li Haoyi", true))
    )

    test("nestedTuples") - checker(
      query = Text {
        Buyer.select
          .join(ShippingInfo)(_.id === _.buyerId)
          .sortBy(_._1.id)
          .map { case (b, s) => (b.id, (b, (s.id, s))) }
      },
      sql = """
        SELECT
          buyer0.id AS res_0,
          buyer0.id AS res_1_0_id,
          buyer0.name AS res_1_0_name,
          buyer0.date_of_birth AS res_1_0_date_of_birth,
          shipping_info1.id AS res_1_1_0,
          shipping_info1.id AS res_1_1_1_id,
          shipping_info1.buyer_id AS res_1_1_1_buyer_id,
          shipping_info1.shipping_date AS res_1_1_1_shipping_date
        FROM buyer buyer0
        JOIN shipping_info shipping_info1 ON (buyer0.id = shipping_info1.buyer_id)
        ORDER BY res_1_0_id
      """,
      value = Seq[(Int, (Buyer[Sc], (Int, ShippingInfo[Sc])))](
        (
          1,
          (
            Buyer[Sc](1, "James Bond", LocalDate.parse("2001-02-03")),
            (2, ShippingInfo[Sc](2, 1, LocalDate.parse("2012-04-05")))
          )
        ),
        (
          2,
          (
            Buyer[Sc](2, "叉烧包", LocalDate.parse("1923-11-12")),
            (1, ShippingInfo[Sc](1, 2, LocalDate.parse("2010-02-03")))
          )
        ),
        (
          2,
          (
            Buyer[Sc](2, "叉烧包", LocalDate.parse("1923-11-12")),
            (3, ShippingInfo[Sc](3, 2, LocalDate.parse("2012-05-06")))
          )
        )
      ),
      docs = """
        Queries can output arbitrarily nested tuples of `Expr[T]` and `case class`
        instances of `Foo[Expr]`, which will be de-serialized into nested tuples
        of `T` and `Foo[Sc]`s. The `AS` aliases assigned to each column will contain
        the path of indices and field names used to populate the final returned values
      """
    )

    test("case") {
      test("when") - checker(
        query = Text {
          Product.select.map(p =>
            db.caseWhen(
              (p.price > 200) -> (p.name + " EXPENSIVE"),
              (p.price > 5) -> (p.name + " NORMAL"),
              (p.price <= 5) -> (p.name + " CHEAP")
            )
          )
        },
        sqls = Seq(
          """
            SELECT
              CASE
                WHEN (product0.price > ?) THEN (product0.name || ?)
                WHEN (product0.price > ?) THEN (product0.name || ?)
                WHEN (product0.price <= ?) THEN (product0.name || ?)
              END AS res
            FROM product product0
          """,
          """
            SELECT
              CASE
                WHEN (product0.price > ?) THEN CONCAT(product0.name, ?)
                WHEN (product0.price > ?) THEN CONCAT(product0.name, ?)
                WHEN (product0.price <= ?) THEN CONCAT(product0.name, ?)
              END AS res
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
        ),
        docs = """
          ScalaSql's `caseWhen` method translates into SQL's `CASE`/`WHEN`/`ELSE`/`END` syntax,
          allowing you to perform basic conditionals as part of your SQL query
        """
      )

      test("else") - checker(
        query = Text {
          Product.select.map(p =>
            db.caseWhen(
              (p.price > 200) -> (p.name + " EXPENSIVE"),
              (p.price > 5) -> (p.name + " NORMAL")
            ).`else` { p.name + " UNKNOWN" }
          )
        },
        sqls = Seq(
          """
            SELECT
              CASE
                WHEN (product0.price > ?) THEN (product0.name || ?)
                WHEN (product0.price > ?) THEN (product0.name || ?)
                ELSE (product0.name || ?)
              END AS res
            FROM product product0
          """,
          """
            SELECT
              CASE
                WHEN (product0.price > ?) THEN CONCAT(product0.name, ?)
                WHEN (product0.price > ?) THEN CONCAT(product0.name, ?)
                ELSE CONCAT(product0.name, ?)
              END AS res
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
