package scalasql.query

import scalasql._
import sourcecode.Text
import utest._
import utils.ScalaSqlSuite

import java.time.LocalDate

trait LateralJoinTests extends ScalaSqlSuite {
  implicit def LateralJoinOpsConv[C[_, _], Q, R](wrapped: JoinOps[C, Q, R]): LateralJoinOps[C, Q, R]
  def description = "`JOIN LATERAL`, for the databases that support it"
  def tests = Tests {

    test("crossJoinLateral") - checker(
      query = Text {
        Buyer.select
          .crossJoinLateral(b => ShippingInfo.select.filter { s => b.id `=` s.buyerId })
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
          s <- ShippingInfo.select.filter { s => b.id `=` s.buyerId }.crossJoin().lateral
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
          .joinLateral(b => ShippingInfo.select.filter { s => b.id `=` s.buyerId })((_, _) => true)
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
          s <- ShippingInfo.select.filter { s => b.id `=` s.buyerId }.join(_ => true).lateral
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

  }
}
