package scalasql.query

import scalasql._
import utest._
import utils.ScalaSqlSuite

import java.time.LocalDate

/**
 * Tests for joins
 */
trait JoinTests extends ScalaSqlSuite {
  def tests = Tests {
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
}
