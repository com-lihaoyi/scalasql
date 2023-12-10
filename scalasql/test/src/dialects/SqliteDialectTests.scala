package scalasql.dialects

import scalasql._
import scalasql.core.Db
import utest._
import utils.SqliteSuite

trait SqliteDialectTests extends SqliteSuite {
  def description = "Operations specific to working with Sqlite Databases"
  def tests = Tests {

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

    test("glob") - checker(
      query = Db("*cop*").glob("roflcopter"),
      sql = "SELECT GLOB(?, ?) AS res",
      value = true
    )


    test("changes") - checker(
      query = db.changes,
      sql = "SELECT CHANGES() AS res",
      value = 7
    )

    test("char") - checker(
      query = db.char(108, 111, 108),
      sql = "SELECT CHAR(?, ?, ?) AS res",
      value = "lol"
    )

  }
}
