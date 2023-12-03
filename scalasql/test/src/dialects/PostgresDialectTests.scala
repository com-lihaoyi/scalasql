package scalasql.dialects

import scalasql._
import scalasql.core.Sql
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
          DISTINCT ON (purchase0.shipping_info_id) purchase0.id AS res__id,
          purchase0.shipping_info_id AS res__shipping_info_id,
          purchase0.product_id AS res__product_id,
          purchase0.count AS res__count,
          purchase0.total AS res__total
        FROM purchase purchase0
        ORDER BY res__shipping_info_id DESC
      """,
      value = Seq(
        Purchase[Id](6, 3, 1, 5, 44.4),
        Purchase[Id](4, 2, 4, 4, 493.8),
        Purchase[Id](2, 1, 2, 3, 900.0)
      ),
      docs = """
        ScalaSql's Postgres dialect provides teh `.distinctOn` operator, which translates
        into a SQL `DISTINCT ON` clause
      """
    )

    test("ltrim2") - checker(
      query = Sql("xxHellox").ltrim("x"),
      sql = "SELECT LTRIM(?, ?) AS res",
      value = "Hellox"
    )

    test("rtrim2") - checker(
      query = Sql("xxHellox").rtrim("x"),
      sql = "SELECT RTRIM(?, ?) AS res",
      value = "xxHello"
    )

    test("reverse") -
      checker(query = Sql("Hello").reverse, sql = "SELECT REVERSE(?) AS res", value = "olleH")

    test("lpad") - checker(
      query = Sql("Hello").lpad(10, "xy"),
      sql = "SELECT LPAD(?, ?, ?) AS res",
      value = "xyxyxHello"
    )

    test("rpad") - checker(
      query = Sql("Hello").rpad(10, "xy"),
      sql = "SELECT RPAD(?, ?, ?) AS res",
      value = "Helloxyxyx"
    )
  }
}
