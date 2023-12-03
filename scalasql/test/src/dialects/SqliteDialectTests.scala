package scalasql.dialects

import scalasql._
import scalasql.core.Sql
import utest._
import utils.SqliteSuite

trait SqliteDialectTests extends SqliteSuite {
  def description = "Operations specific to working with Sqlite Databases"
  def tests = Tests {

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

  }
}
