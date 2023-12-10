package scalasql.dialects

import scalasql._
import scalasql.core.Expr
import utest._
import utils.SqliteSuite

trait SqliteDialectTests extends SqliteSuite {
  def description = "Operations specific to working with Sqlite Databases"
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

    test("glob") - checker(
      query = Expr("*cop*").glob("roflcopter"),
      sql = "SELECT GLOB(?, ?) AS res",
      value = true
    )

    test("changes") - checker(
      query = db.changes,
      sql = "SELECT CHANGES() AS res"
    )

    test("totalChanges") - checker(
      query = db.totalChanges,
      sql = "SELECT TOTAL_CHANGES() AS res"
    )

    test("typeOf") - checker(
      query = db.typeOf(123),
      sql = "SELECT TYPEOF(?) AS res",
      value = "integer"
    )

    test("lastInsertRowId") - checker(
      query = db.lastInsertRowId,
      sql = "SELECT LAST_INSERT_ROWID() AS res"
    )

    test("char") - checker(
      query = db.char(108, 111, 108),
      sql = "SELECT CHAR(?, ?, ?) AS res",
      value = "lol"
    )

    test("format") - checker(
      query = db.format("i am cow %s hear me moo %s", 1337, 31337),
      sql = "SELECT FORMAT(?, ?, ?) AS res",
      value = "i am cow 1337 hear me moo 31337"
    )

    test("hex") - checker(
      query = db.hex(new geny.Bytes(Array(1, 10, 100, -127))),
      sql = "SELECT HEX(?) AS res",
      value = "010A6481"
    )

    test("unhex") - checker(
      query = db.unhex("010A6481"),
      sql = "SELECT UNHEX(?) AS res",
      value = new geny.Bytes(Array(1, 10, 100, -127))
    )

    test("zeroBlob") - checker(
      query = db.zeroBlob(16),
      sql = "SELECT ZEROBLOB(?) AS res",
      value = new geny.Bytes(new Array[Byte](16))
    )
  }
}
