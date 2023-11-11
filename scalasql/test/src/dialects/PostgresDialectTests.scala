package scalasql.dialects

import scalasql._
import scalasql.query.Expr
import utest._
import utils.PostgresSuite

trait PostgresDialectTests extends PostgresSuite {
  def description = "Operations specific to working with Postgres Databases"
  def tests = Tests {

    test("ltrim2") - checker(
      query = Expr("xxHellox").ltrim("x"),
      sql = "SELECT LTRIM(?, ?) as res",
      value = "Hellox"
    )

    test("rtrim2") - checker(
      query = Expr("xxHellox").rtrim("x"),
      sql = "SELECT RTRIM(?, ?) as res",
      value = "xxHello"
    )

    test("reverse") -
      checker(query = Expr("Hello").reverse, sql = "SELECT REVERSE(?) as res", value = "olleH")

    test("lpad") - checker(
      query = Expr("Hello").lpad(10, "xy"),
      sql = "SELECT LPAD(?, ?, ?) as res",
      value = "xyxyxHello"
    )

    test("rpad") - checker(
      query = Expr("Hello").rpad(10, "xy"),
      sql = "SELECT RPAD(?, ?, ?) as res",
      value = "Helloxyxyx"
    )
  }
}
