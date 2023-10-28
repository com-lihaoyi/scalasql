package scalasql.dialects

import scalasql._
import scalasql.query.Expr
import utest._
import utils.SqliteSuite

trait SqliteDialectTests extends SqliteSuite {
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

  }
}
