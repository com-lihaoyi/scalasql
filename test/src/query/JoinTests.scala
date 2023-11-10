package scalasql.query

import scalasql._
import sourcecode.Text
import utest._
import utils.ScalaSqlSuite

import java.time.LocalDate

trait JoinTests extends ScalaSqlSuite {
  def description = "inner `JOIN`s, `JOIN ON`s, self-joins, `LEFT`/`RIGHT`/`OUTER` `JOIN`s"
  def tests = Tests {
    test("joinFilter") - checker(
      query = Text {
        Buyer.select.join(ShippingInfo)(_.id `=` _.buyerId).filter(_._1.name `=` "叉烧包")
      },
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
      ),
      docs = """
        ScalaSql's `.join` or `.join` methods correspond to SQL `JOIN` and `JOIN ... ON ...`.
        These perform an inner join between two tables, with an optional `ON` predicate. You can
        also `.filter` and `.map` the results of the join, making use of the columns joined from
        the two tables
      """
    )

    test("joinFilterMap") - checker(
      query = Text {
        Buyer.select
          .join(ShippingInfo)(_.id `=` _.buyerId)
          .filter(_._1.name `=` "James Bond")
          .map(_._2.shippingDate)
      },
      sql = """
        SELECT shipping_info1.shipping_date as res
        FROM buyer buyer0
        JOIN shipping_info shipping_info1 ON buyer0.id = shipping_info1.buyer_id
        WHERE buyer0.name = ?
      """,
      value = Seq(LocalDate.parse("2012-04-05"))
    )

    test("selfJoin") - checker(
      query = Text { Buyer.select.join(Buyer)(_.id `=` _.id) },
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
      ),
      docs = """
        ScalaSql supports a "self join", where a table is joined with itself. This
        is done by simply having the same table be on the left-hand-side and right-hand-side
        of your `.join` or `.join` method. The two example self-joins below are trivial,
        but illustrate how to do it in case you want to do a self-join in a more realistic setting.
      """
    )

    test("selfJoin2") - checker(
      query = Text { Buyer.select.join(Buyer)(_.id <> _.id) },
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
      query = Text {
        Buyer.select
          .flatMap(b => ShippingInfo.select.map((b, _)))
          .filter { case (b, s) => b.id `=` s.buyerId && b.name `=` "James Bond" }
          .map(_._2.shippingDate)
      },
      sql = """
        SELECT shipping_info1.shipping_date as res
        FROM buyer buyer0
        CROSS JOIN shipping_info shipping_info1
        WHERE buyer0.id = shipping_info1.buyer_id AND buyer0.name = ?
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
          s <- ShippingInfo.select
          if b.id `=` s.buyerId && b.name `=` "James Bond"
        } yield s.shippingDate
      },
      sql = """
        SELECT shipping_info1.shipping_date as res
        FROM buyer buyer0
        CROSS JOIN shipping_info shipping_info1
        WHERE buyer0.id = shipping_info1.buyer_id AND buyer0.name = ?
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
          s <- ShippingInfo.select.filter(b.id `=` _.buyerId)
        } yield s.shippingDate
      },
      sql = """
        SELECT shipping_info1.shipping_date as res
        FROM buyer buyer0
        CROSS JOIN shipping_info shipping_info1
        WHERE buyer0.name = ? AND buyer0.id = shipping_info1.buyer_id
      """,
      value = Seq(LocalDate.parse("2012-04-05"))
    )

    test("flatMapForJoin") - checker(
      query = Text {
        for {
          (b, si) <- Buyer.select.join(ShippingInfo)(_.id `=` _.buyerId)
          (pu, pr) <- Purchase.select.join(Product)(_.productId `=` _.id)
          if si.id `=` pu.shippingInfoId
        } yield (b.name, pr.name)
      },
      sql = """
        SELECT buyer0.name as res__0, product3.name as res__1
        FROM buyer buyer0
        JOIN shipping_info shipping_info1 ON buyer0.id = shipping_info1.buyer_id
        CROSS JOIN purchase purchase2
        JOIN product product3 ON purchase2.product_id = product3.id
        WHERE shipping_info1.id = purchase2.shipping_info_id
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
          shippingInfo <- ShippingInfo.select
        } yield (name, dateOfBirth, shippingInfo.id, shippingInfo.shippingDate)
      },
      sql = """
        SELECT
          subquery0.res__0 as res__0,
          subquery0.res__1 as res__1,
          shipping_info1.id as res__2,
          shipping_info1.shipping_date as res__3
        FROM (SELECT buyer0.name as res__0, MIN(buyer0.date_of_birth) as res__1
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
          (shippingInfoId, shippingDate) <- ShippingInfo.select.groupBy(_.id)(_.minBy(_.shippingDate))
        } yield (name, dateOfBirth, shippingInfoId, shippingDate)
      },
      sql = """
        SELECT
          subquery0.res__0 as res__0,
          subquery0.res__1 as res__1,
          shipping_info1.id as res__2,
          shipping_info1.shipping_date as res__3
        FROM (SELECT buyer0.name as res__0, MIN(buyer0.date_of_birth) as res__1
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

    test("mapForGroupBy") - checker(
      query = Text {
        for ((name, dateOfBirth) <- Buyer.select.groupBy(_.name)(_.minBy(_.dateOfBirth)))
          yield (name, dateOfBirth)
      },
      sql = """
        SELECT buyer0.name as res__0, MIN(buyer0.date_of_birth) as res__1
        FROM buyer buyer0
        GROUP BY buyer0.name
      """,
      value = Seq(
        ("James Bond", LocalDate.parse("2001-02-03")),
        ("Li Haoyi", LocalDate.parse("1965-08-09")),
        ("叉烧包", LocalDate.parse("1923-11-12"))
      ),
      docs = """
        Using non-trivial queries in the `for`-comprehension may result in subqueries
        being generated
      """,
      normalize = (x: Seq[(String, LocalDate)]) => x.sortBy(_._1)
    )

    test("leftJoin") - checker(
      query = Text { Buyer.select.leftJoin(ShippingInfo)(_.id `=` _.buyerId) },
      sql = """
        SELECT
          buyer0.id as res__0__id,
          buyer0.name as res__0__name,
          buyer0.date_of_birth as res__0__date_of_birth,
          shipping_info1.id as res__1__id,
          shipping_info1.buyer_id as res__1__buyer_id,
          shipping_info1.shipping_date as res__1__shipping_date
        FROM buyer buyer0
        LEFT JOIN shipping_info shipping_info1 ON buyer0.id = shipping_info1.buyer_id
      """,
      value = Seq(
        (
          Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")),
          Some(ShippingInfo[Id](2, 1, LocalDate.parse("2012-04-05")))
        ),
        (
          Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
          Some(ShippingInfo[Id](1, 2, LocalDate.parse("2010-02-03")))
        ),
        (
          Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
          Some(ShippingInfo[Id](3, 2, LocalDate.parse("2012-05-06")))
        ),
        (Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09")), None)
      ),
      normalize =
        (x: Seq[(Buyer[Id], Option[ShippingInfo[Id]])]) => x.sortBy(t => t._1.id -> t._2.map(_.id)),
      docs = """
        ScalaSql supports `LEFT JOIN`s, `RIGHT JOIN`s and `OUTER JOIN`s via the
        `.leftJoin`/`.rightJoin`/`.outerJoin` methods
      """
    )

    test("leftJoinMap") - checker(
      query = Text {
        Buyer.select
          .leftJoin(ShippingInfo)(_.id `=` _.buyerId)
          .map { case (b, si) => (b.name, si.map(_.shippingDate)) }
      },
      sql = """
        SELECT buyer0.name as res__0, shipping_info1.shipping_date as res__1
        FROM buyer buyer0
        LEFT JOIN shipping_info shipping_info1 ON buyer0.id = shipping_info1.buyer_id
      """,
      value = Seq(
        ("James Bond", Some(LocalDate.parse("2012-04-05"))),
        ("Li Haoyi", None),
        ("叉烧包", Some(LocalDate.parse("2010-02-03"))),
        ("叉烧包", Some(LocalDate.parse("2012-05-06")))
      ),
      normalize =
        (x: Seq[(String, Option[LocalDate])]) => x.sortBy(t => t._1 -> t._2.map(_.toEpochDay)),
      docs = """
        `.leftJoin`s return a `Nullable[Q]` for the right hand entry. This is similar
        to `Option[Q]` in Scala, supports a similar set of operations (e.g. `.map`),
        and becomes an `Option[Q]` after the query is executed
      """
    )

    test("leftJoinExpr") - checker(
      query = Text {
        Buyer.select
          .leftJoin(ShippingInfo)(_.id `=` _.buyerId)
          .map { case (b, si) => (b.name, si.map(_.shippingDate)) }
          .sortBy(_._2)
          .nullsFirst
      },
      sqls = Seq(
        """
          SELECT buyer0.name as res__0, shipping_info1.shipping_date as res__1
          FROM buyer buyer0
          LEFT JOIN shipping_info shipping_info1 ON buyer0.id = shipping_info1.buyer_id
          ORDER BY res__1 NULLS FIRST
        """,
        // MySQL doesn't support NULLS FIRST syntax and needs a workaround
        """
          SELECT buyer0.name as res__0, shipping_info1.shipping_date as res__1
          FROM buyer buyer0
          LEFT JOIN shipping_info shipping_info1 ON buyer0.id = shipping_info1.buyer_id
          ORDER BY res__1 IS NULL DESC, res__1
        """
      ),
      value = Seq(
        ("Li Haoyi", None),
        ("叉烧包", Some(LocalDate.parse("2010-02-03"))),
        ("James Bond", Some(LocalDate.parse("2012-04-05"))),
        ("叉烧包", Some(LocalDate.parse("2012-05-06")))
      ),
      docs = """
        `Nullable[Expr[T]]`s can be implicitly used as `Expr[Option[T]]`s. This allows
        them to participate in any database query logic than any other `Expr[Option[T]]`s
        can participate in, such as being used as sort key or in computing return values
        (below).
      """
    )

    test("leftJoinExpr2") - checker(
      query = Text {
        Buyer.select
          .leftJoin(ShippingInfo)(_.id `=` _.buyerId)
          .map { case (b, si) => (b.name, si.map(_.shippingDate) > b.dateOfBirth) }
      },
      sql = """
        SELECT
          buyer0.name as res__0,
          shipping_info1.shipping_date > buyer0.date_of_birth as res__1
        FROM buyer buyer0
        LEFT JOIN shipping_info shipping_info1 ON buyer0.id = shipping_info1.buyer_id
      """,
      value = Seq(
        ("James Bond", true),
        ("Li Haoyi", false),
        ("叉烧包", true),
        ("叉烧包", true)
      ),
      normalize = (x: Seq[(String, Boolean)]) => x.sorted
    )
    test("leftJoinExprExplicit") - checker(
      query = Text {
        Buyer.select
          .leftJoin(ShippingInfo)(_.id `=` _.buyerId)
          .map { case (b, si) => (b.name, Nullable.toExpr(si.map(_.shippingDate)) > b.dateOfBirth) }
      },
      sql = """
        SELECT
          buyer0.name as res__0,
          shipping_info1.shipping_date > buyer0.date_of_birth as res__1
        FROM buyer buyer0
        LEFT JOIN shipping_info shipping_info1 ON buyer0.id = shipping_info1.buyer_id
      """,
      value = Seq(
        ("James Bond", true),
        ("Li Haoyi", false),
        ("叉烧包", true),
        ("叉烧包", true)
      ),
      normalize = (x: Seq[(String, Boolean)]) => x.sorted,
      docs = """
        The conversion from `Nullable[T]` to `Expr[Option[T]]` can also be performed
        explicitly via `Nullable.toExpr(...)`
      """
    )

    test("rightJoin") - checker(
      query = Text { ShippingInfo.select.rightJoin(Buyer)(_.buyerId `=` _.id) },
      sql = """
        SELECT
          shipping_info0.id as res__0__id,
          shipping_info0.buyer_id as res__0__buyer_id,
          shipping_info0.shipping_date as res__0__shipping_date,
          buyer1.id as res__1__id,
          buyer1.name as res__1__name,
          buyer1.date_of_birth as res__1__date_of_birth
        FROM shipping_info shipping_info0
        RIGHT JOIN buyer buyer1 ON shipping_info0.buyer_id = buyer1.id
      """,
      value = Seq(
        (
          Some(ShippingInfo[Id](2, 1, LocalDate.parse("2012-04-05"))),
          Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03"))
        ),
        (
          Some(ShippingInfo[Id](1, 2, LocalDate.parse("2010-02-03"))),
          Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12"))
        ),
        (
          Some(ShippingInfo[Id](3, 2, LocalDate.parse("2012-05-06"))),
          Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12"))
        ),
        (None, Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09")))
      ),
      normalize =
        (x: Seq[(Option[ShippingInfo[Id]], Buyer[Id])]) => x.sortBy(t => t._2.id -> t._1.map(_.id))
    )

    test("outerJoin") - checker(
      query = Text { ShippingInfo.select.outerJoin(Buyer)(_.buyerId `=` _.id) },
      sqls = Seq(
        """
          SELECT
            shipping_info0.id as res__0__id,
            shipping_info0.buyer_id as res__0__buyer_id,
            shipping_info0.shipping_date as res__0__shipping_date,
            buyer1.id as res__1__id,
            buyer1.name as res__1__name,
            buyer1.date_of_birth as res__1__date_of_birth
          FROM shipping_info shipping_info0
          FULL OUTER JOIN buyer buyer1 ON shipping_info0.buyer_id = buyer1.id
        """,
        """
          SELECT
            shipping_info0.id as res__0__id,
            shipping_info0.buyer_id as res__0__buyer_id,
            shipping_info0.shipping_date as res__0__shipping_date,
            buyer1.id as res__1__id,
            buyer1.name as res__1__name,
            buyer1.date_of_birth as res__1__date_of_birth
          FROM shipping_info shipping_info0
          LEFT JOIN buyer buyer1 ON shipping_info0.buyer_id = buyer1.id
          UNION
          SELECT
            shipping_info0.id as res__0__id,
            shipping_info0.buyer_id as res__0__buyer_id,
            shipping_info0.shipping_date as res__0__shipping_date,
            buyer1.id as res__1__id,
            buyer1.name as res__1__name,
            buyer1.date_of_birth as res__1__date_of_birth
          FROM shipping_info shipping_info0
          RIGHT JOIN buyer buyer1 ON shipping_info0.buyer_id = buyer1.id"""
      ),
      value = Seq(
        (
          Option(ShippingInfo[Id](2, 1, LocalDate.parse("2012-04-05"))),
          Option(Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")))
        ),
        (
          Option(ShippingInfo[Id](1, 2, LocalDate.parse("2010-02-03"))),
          Option(Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")))
        ),
        (
          Option(ShippingInfo[Id](3, 2, LocalDate.parse("2012-05-06"))),
          Option(Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")))
        ),
        (Option.empty, Option(Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09"))))
      ),
      normalize = (x: Seq[(Option[ShippingInfo[Id]], Option[Buyer[Id]])]) =>
        x.sortBy(t => t._2.map(_.id) -> t._1.map(_.id))
    )

    test("crossJoin") - checker(
      query = Text {
        Buyer.select
          .crossJoin(ShippingInfo)
          .filter { case (b, s) => b.id `=` s.buyerId }
          .map { case (b, s) => (b.name, s.shippingDate) }
      },
      sql = """
        SELECT buyer0.name as res__0, shipping_info1.shipping_date as res__1
        FROM buyer buyer0
        CROSS JOIN shipping_info shipping_info1
        WHERE buyer0.id = shipping_info1.buyer_id
        """,
      value = Seq(
        ("James Bond", LocalDate.parse("2012-04-05")),
        ("叉烧包", LocalDate.parse("2010-02-03")),
        ("叉烧包", LocalDate.parse("2012-05-06"))
      ),
      docs = """
        `.crossJoin` can be used to generate a SQL `CROSS JOIN`, which allows you
        to perform a `JOIN` with an `ON` clause in a consistent way across databases
      """,
      normalize = (x: Seq[(String, LocalDate)]) => x.sortBy(t => (t._1, t._2.toEpochDay))
    )

    test("flatJoins") {
      test("join") - checker(
        query = Text {
          for {
            b <- Buyer.select
            si <- ShippingInfo.join(_.buyerId `=` b.id)
          } yield (b.name, si.shippingDate)
        },
        sql = """
          SELECT buyer0.name as res__0, shipping_info1.shipping_date as res__1
          FROM buyer buyer0
          JOIN shipping_info shipping_info1 ON shipping_info1.buyer_id = buyer0.id
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
          SELECT buyer0.name as res__0, product3.name as res__1, product3.price as res__2
          FROM buyer buyer0
          JOIN shipping_info shipping_info1 ON shipping_info1.id = buyer0.id
          JOIN purchase purchase2 ON purchase2.shipping_info_id = shipping_info1.id
          JOIN product product3 ON product3.id = purchase2.product_id
          WHERE buyer0.name = ? AND product3.price > ?
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
          SELECT buyer0.name as res__0, shipping_info1.shipping_date as res__1
          FROM buyer buyer0
          LEFT JOIN shipping_info shipping_info1 ON shipping_info1.buyer_id = buyer0.id
        """,
        value = Seq(
          ("James Bond", Some(LocalDate.parse("2012-04-05"))),
          ("Li Haoyi", None),
          ("叉烧包", Some(LocalDate.parse("2010-02-03"))),
          ("叉烧包", Some(LocalDate.parse("2012-05-06")))
        ),
        docs = """
          Flat joins can also support `.leftJoin`s, where the table being joined
          is given to you as a `Nullable[T]`
        """,
        normalize = (x: Seq[(String, Option[LocalDate])]) => x.sortBy(t => (t._1, t._2.map(_.toEpochDay)))
      )
    }
  }
}
