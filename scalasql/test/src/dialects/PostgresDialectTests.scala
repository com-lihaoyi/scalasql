package scalasql.dialects

import scalasql._
import scalasql.core.Db
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
      query = Db("xxHellox").ltrim("x"),
      sql = "SELECT LTRIM(?, ?) AS res",
      value = "Hellox"
    )

    test("rtrim2") - checker(
      query = Db("xxHellox").rtrim("x"),
      sql = "SELECT RTRIM(?, ?) AS res",
      value = "xxHello"
    )

    test("reverse") -
      checker(query = Db("Hello").reverse, sql = "SELECT REVERSE(?) AS res", value = "olleH")

    test("lpad") - checker(
      query = Db("Hello").lpad(10, "xy"),
      sql = "SELECT LPAD(?, ?, ?) AS res",
      value = "xyxyxHello"
    )

    test("rpad") - checker(
      query = Db("Hello").rpad(10, "xy"),
      sql = "SELECT RPAD(?, ?, ?) AS res",
      value = "Helloxyxyx"
    )
  }
}
