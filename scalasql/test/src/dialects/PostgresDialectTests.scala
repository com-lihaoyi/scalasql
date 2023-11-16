package scalasql.dialects

import scalasql._
import scalasql.query.Expr
import sourcecode.Text
import utest._
import utils.PostgresSuite

import java.time.LocalDate

trait PostgresDialectTests extends PostgresSuite {
  def description = "Operations specific to working with Postgres Databases"
  def tests = Tests {

    test("ltrim2") - checker(
      query = Expr("xxHellox").ltrim("x"),
      sql = "SELECT LTRIM(?, ?) AS res",
      value = "Hellox"
    )

    test("rtrim2") - checker(
      query = Expr("xxHellox").rtrim("x"),
      sql = "SELECT RTRIM(?, ?) AS res",
      value = "xxHello"
    )

    test("reverse") -
      checker(query = Expr("Hello").reverse, sql = "SELECT REVERSE(?) AS res", value = "olleH")

    test("lpad") - checker(
      query = Expr("Hello").lpad(10, "xy"),
      sql = "SELECT LPAD(?, ?, ?) AS res",
      value = "xyxyxHello"
    )

    test("rpad") - checker(
      query = Expr("Hello").rpad(10, "xy"),
      sql = "SELECT RPAD(?, ?, ?) AS res",
      value = "Helloxyxyx"
    )

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

  }
}
