package scalasql.query

import scalasql._
import sourcecode.Text
import utest._
import utils.ScalaSqlSuite

import java.time.LocalDate

trait LateralJoinTests extends ScalaSqlSuite {
  implicit def LateralJoinOpsConv[C[_, _], Q, R](wrapped: JoinOps[C, Q, R] & Joinable[Q, R])(
      implicit qr: Queryable.Row[Q, R]
  ): LateralJoinOps[C, Q, R]
  def description = """
    `JOIN LATERAL`, for the databases that support it. This allows you to use the
    expressions defined in tables on the left-hand-side of the join in a
    subquery on the right-hand-side of the join, v.s. normal `JOIN`s which only
    allow you to use left-hand-side expressions in the `ON` expression but not
    in the `FROM` subquery.
  """
  def tests = Tests {

    test("crossJoinLateral") - checker(
      query = Text {
        Buyer.select
          .crossJoinLateral(b => ShippingInfo.select.filter { s => b.id `=` s.buyerId })
          .map { case (b, s) => (b.name, s.shippingDate) }
      },
      sql = """
        SELECT buyer0.name AS res_0, subquery1.shipping_date AS res_1
        FROM buyer buyer0
        CROSS JOIN LATERAL (SELECT shipping_info1.shipping_date AS shipping_date
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
        for {
          b <- Buyer.select
          s <- ShippingInfo.select.filter { s => b.id `=` s.buyerId }.crossJoinLateral()
        } yield (b.name, s.shippingDate)
      },
      sql = """
        SELECT buyer0.name AS res_0, subquery1.shipping_date AS res_1
        FROM buyer buyer0
        CROSS JOIN LATERAL (SELECT shipping_info1.shipping_date AS shipping_date
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
          .joinLateral(b => ShippingInfo.select.filter { s => b.id `=` s.buyerId })((_, _) => true)
          .map { case (b, s) => (b.name, s.shippingDate) }
      },
      sql = """
        SELECT buyer0.name AS res_0, subquery1.shipping_date AS res_1
        FROM buyer buyer0
        JOIN LATERAL (SELECT shipping_info1.shipping_date AS shipping_date
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
          s <- ShippingInfo.select.filter { s => b.id `=` s.buyerId }.joinLateral(_ => Expr(true))
        } yield (b.name, s.shippingDate)
      },
      sql = """
        SELECT buyer0.name AS res_0, subquery1.shipping_date AS res_1
        FROM buyer buyer0
        JOIN LATERAL (SELECT shipping_info1.shipping_date AS shipping_date
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

    test("leftJoin") - checker(
      query = Text {
        Buyer.select.leftJoinLateral(b => ShippingInfo.select.filter(b.id `=` _.buyerId))((_, _) =>
          Expr(true)
        )
      },
      sql = """
        SELECT
          buyer0.id AS res_0_id,
          buyer0.name AS res_0_name,
          buyer0.date_of_birth AS res_0_date_of_birth,
          subquery1.id AS res_1_id,
          subquery1.buyer_id AS res_1_buyer_id,
          subquery1.shipping_date AS res_1_shipping_date
        FROM buyer buyer0
        LEFT JOIN LATERAL (SELECT
            shipping_info1.id AS id,
            shipping_info1.buyer_id AS buyer_id,
            shipping_info1.shipping_date AS shipping_date
          FROM shipping_info shipping_info1
          WHERE (buyer0.id = shipping_info1.buyer_id)) subquery1 ON ?
      """,
      value = Seq(
        (
          Buyer[Sc](1, "James Bond", LocalDate.parse("2001-02-03")),
          Some(ShippingInfo[Sc](2, 1, LocalDate.parse("2012-04-05")))
        ),
        (
          Buyer[Sc](2, "叉烧包", LocalDate.parse("1923-11-12")),
          Some(ShippingInfo[Sc](1, 2, LocalDate.parse("2010-02-03")))
        ),
        (
          Buyer[Sc](2, "叉烧包", LocalDate.parse("1923-11-12")),
          Some(ShippingInfo[Sc](3, 2, LocalDate.parse("2012-05-06")))
        ),
        (Buyer[Sc](3, "Li Haoyi", LocalDate.parse("1965-08-09")), None)
      ),
      normalize =
        (x: Seq[(Buyer[Sc], Option[ShippingInfo[Sc]])]) => x.sortBy(t => t._1.id -> t._2.map(_.id)),
      docs = """
        ScalaSql supports `LEFT JOIN`s, `RIGHT JOIN`s and `OUTER JOIN`s via the
        `.leftJoin`/`.rightJoin`/`.outerJoin` methods
      """
    )

    test("leftJoinFor") - checker(
      query = Text {
        for {
          b <- Buyer.select
          s <- ShippingInfo.select.filter(b.id `=` _.buyerId).leftJoinLateral(_ => Expr(true))
        } yield (b, s)
      },
      sql = """
        SELECT
          buyer0.id AS res_0_id,
          buyer0.name AS res_0_name,
          buyer0.date_of_birth AS res_0_date_of_birth,
          subquery1.id AS res_1_id,
          subquery1.buyer_id AS res_1_buyer_id,
          subquery1.shipping_date AS res_1_shipping_date
        FROM buyer buyer0
        LEFT JOIN LATERAL (SELECT
            shipping_info1.id AS id,
            shipping_info1.buyer_id AS buyer_id,
            shipping_info1.shipping_date AS shipping_date
          FROM shipping_info shipping_info1
          WHERE (buyer0.id = shipping_info1.buyer_id)) subquery1 ON ?
      """,
      value = Seq(
        (
          Buyer[Sc](1, "James Bond", LocalDate.parse("2001-02-03")),
          Some(ShippingInfo[Sc](2, 1, LocalDate.parse("2012-04-05")))
        ),
        (
          Buyer[Sc](2, "叉烧包", LocalDate.parse("1923-11-12")),
          Some(ShippingInfo[Sc](1, 2, LocalDate.parse("2010-02-03")))
        ),
        (
          Buyer[Sc](2, "叉烧包", LocalDate.parse("1923-11-12")),
          Some(ShippingInfo[Sc](3, 2, LocalDate.parse("2012-05-06")))
        ),
        (Buyer[Sc](3, "Li Haoyi", LocalDate.parse("1965-08-09")), None)
      ),
      normalize =
        (x: Seq[(Buyer[Sc], Option[ShippingInfo[Sc]])]) => x.sortBy(t => t._1.id -> t._2.map(_.id)),
      docs = """
        ScalaSql supports `LEFT JOIN`s, `RIGHT JOIN`s and `OUTER JOIN`s via the
        `.leftJoin`/`.rightJoin`/`.outerJoin` methods
      """
    )

  }
}
