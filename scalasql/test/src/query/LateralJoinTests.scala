package scalasql.query

import scalasql._
import sourcecode.Text
import utest._
import utils.ScalaSqlSuite

import java.time.LocalDate

trait LateralJoinTests extends ScalaSqlSuite {
  implicit def LateralJoinOpsConv[C[_, _], Q, R](wrapped: JoinOps[C, Q, R] with Joinable[Q, R]): LateralJoinOps[C, Q, R]
  def description = "`JOIN LATERAL`, for the databases that support it"
  def tests = Tests {

    test("crossJoinLateral") - checker(
      query = Text {
        Buyer.select
          .lateral.crossJoin(b => ShippingInfo.select.filter { s => b.id `=` s.buyerId })
          .map { case (b, s) => (b.name, s.shippingDate) }
      },
      sql = """
        SELECT buyer0.name AS res__0, subquery1.res__shipping_date AS res__1
        FROM buyer buyer0
        CROSS JOIN LATERAL (SELECT shipping_info1.shipping_date AS res__shipping_date
          FROM shipping_info shipping_info1
          WHERE (buyer0.id = shipping_info1.buyer_id)) subquery1
        """,
      value = Seq(
        ("James Bond", LocalDate.parse("2012-04-05")),
        ("叉烧包", LocalDate.parse("2010-02-03")),
        ("叉烧包", LocalDate.parse("2012-05-06"))
      ),
      docs = """
      """,
      normalize = (x: Seq[(String, LocalDate)]) => x.sortBy(t => (t._1, t._2.toEpochDay))
    )


    test("crossJoinLateralFor") - checker(
      query = Text {
        for{
          b <- Buyer.select
          s <- ShippingInfo.select.filter { s => b.id `=` s.buyerId }.lateral.crossJoin()
        } yield (b.name, s.shippingDate)
      },
      sql = """
        SELECT buyer0.name AS res__0, subquery1.res__shipping_date AS res__1
        FROM buyer buyer0
        CROSS JOIN LATERAL (SELECT shipping_info1.shipping_date AS res__shipping_date
          FROM shipping_info shipping_info1
          WHERE (buyer0.id = shipping_info1.buyer_id)) subquery1
        """,
      value = Seq(
        ("James Bond", LocalDate.parse("2012-04-05")),
        ("叉烧包", LocalDate.parse("2010-02-03")),
        ("叉烧包", LocalDate.parse("2012-05-06"))
      ),
      docs = """
      """,
      normalize = (x: Seq[(String, LocalDate)]) => x.sortBy(t => (t._1, t._2.toEpochDay))
    )


    test("joinLateral") - checker(
      query = Text {
        Buyer.select
          .lateral.join(b => ShippingInfo.select.filter { s => b.id `=` s.buyerId })((_, _) => true)
          .map { case (b, s) => (b.name, s.shippingDate) }
      },
      sql = """
        SELECT buyer0.name AS res__0, subquery1.res__shipping_date AS res__1
        FROM buyer buyer0
        JOIN LATERAL (SELECT shipping_info1.shipping_date AS res__shipping_date
          FROM shipping_info shipping_info1
          WHERE (buyer0.id = shipping_info1.buyer_id)) subquery1
          ON ?
        """,
      value = Seq(
        ("James Bond", LocalDate.parse("2012-04-05")),
        ("叉烧包", LocalDate.parse("2010-02-03")),
        ("叉烧包", LocalDate.parse("2012-05-06"))
      ),
      docs = """
      """,
      normalize = (x: Seq[(String, LocalDate)]) => x.sortBy(t => (t._1, t._2.toEpochDay))
    )


    test("joinLateralFor") - checker(
      query = Text {
        for {
          b <- Buyer.select
          s <- ShippingInfo.select.filter { s => b.id `=` s.buyerId }.lateral.join(_ => Expr(true))
        } yield (b.name, s.shippingDate)
      },
      sql = """
        SELECT buyer0.name AS res__0, subquery1.res__shipping_date AS res__1
        FROM buyer buyer0
        JOIN LATERAL (SELECT shipping_info1.shipping_date AS res__shipping_date
          FROM shipping_info shipping_info1
          WHERE (buyer0.id = shipping_info1.buyer_id)) subquery1
        ON ?
        """,
      value = Seq(
        ("James Bond", LocalDate.parse("2012-04-05")),
        ("叉烧包", LocalDate.parse("2010-02-03")),
        ("叉烧包", LocalDate.parse("2012-05-06"))
      ),
      docs =
        """
      """,
      normalize = (x: Seq[(String, LocalDate)]) => x.sortBy(t => (t._1, t._2.toEpochDay))
    )

    test("leftJoinFor") - checker(
      query = Text {
        for{
          b <- Buyer.select
          s <- ShippingInfo.select.filter(b.id `=` _.buyerId).lateral.leftJoin(_ => Expr(true))
        } yield (b, s)
      },
      sql = """
        SELECT
          buyer0.id AS res__0__id,
          buyer0.name AS res__0__name,
          buyer0.date_of_birth AS res__0__date_of_birth,
          subquery1.res__id AS res__1__id,
          subquery1.res__buyer_id AS res__1__buyer_id,
          subquery1.res__shipping_date AS res__1__shipping_date
        FROM buyer buyer0
        LEFT JOIN LATERAL (SELECT
            shipping_info1.id AS res__id,
            shipping_info1.buyer_id AS res__buyer_id,
            shipping_info1.shipping_date AS res__shipping_date
          FROM shipping_info shipping_info1
          WHERE (buyer0.id = shipping_info1.buyer_id)) subquery1 ON ?
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
      docs =
        """
        ScalaSql supports `LEFT JOIN`s, `RIGHT JOIN`s and `OUTER JOIN`s via the
        `.leftJoin`/`.rightJoin`/`.outerJoin` methods
      """
    )

  }
}
