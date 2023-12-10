package scalasql.dialects

import scalasql._
import scalasql.core.Expr
import sourcecode.Text
import utest._
import utils.PostgresSuite

import java.time.LocalDate

trait PostgresDialectTests extends PostgresSuite {
  def description = "Operations specific to working with Postgres Databases"
  def tests = Tests {

    test("distinctOn") - checker(
      query = Purchase.select.distinctOn(_.shippingInfoId).sortBy(_.shippingInfoId).desc,
      sql = """
        SELECT
          DISTINCT ON (purchase0.shipping_info_id) purchase0.id AS id,
          purchase0.shipping_info_id AS shipping_info_id,
          purchase0.product_id AS product_id,
          purchase0.count AS count,
          purchase0.total AS total
        FROM purchase purchase0
        ORDER BY shipping_info_id DESC
      """,
      value = Seq(
        Purchase[Sc](6, 3, 1, 5, 44.4),
        Purchase[Sc](4, 2, 4, 4, 493.8),
        Purchase[Sc](2, 1, 2, 3, 900.0)
      ),
      docs = """
        ScalaSql's Postgres dialect provides teh `.distinctOn` operator, which translates
        into a SQL `DISTINCT ON` clause
      """
    )

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

    test("concat") - checker(
      query = db.concat("i ", "am", " cow", 1337),
      sql = "SELECT CONCAT(?, ?, ?, ?) AS res",
      value = "i am cow1337"
    )

    test("concatWs") - checker(
      query = db.concatWs(" ", "i", "am", "cow", 1337),
      sql = "SELECT CONCAT_WS(?, ?, ?, ?, ?) AS res",
      value = "i am cow 1337"
    )

    test("format") - checker(
      query = db.format("i am cow %s hear me moo %s", 1337, 31337),
      sql = "SELECT FORMAT(?, ?, ?) AS res",
      value = "i am cow 1337 hear me moo 31337"
    )

    test("random") - checker(
      query = db.random,
      sql = "SELECT RANDOM() AS res"
    )
  }
}
