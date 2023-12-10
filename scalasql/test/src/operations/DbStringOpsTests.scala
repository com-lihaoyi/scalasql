package scalasql.operations

import scalasql._
import scalasql.core.Db
import utest._
import utils.ScalaSqlSuite

trait DbStringOpsTests extends ScalaSqlSuite {
  def description = "Operations that can be performed on `Db[String]`"
  def tests = Tests {
    test("plus") - checker(
      query = Db("hello") + Db("world"),
      sqls = Seq("SELECT (? || ?) AS res", "SELECT CONCAT(?, ?) AS res"),
      value = "helloworld"
    )
    test("like") -
      checker(query = Db("hello").like("he%"), sql = "SELECT (? LIKE ?) AS res", value = true)
    test("length") -
      checker(query = Db("hello").length, sql = "SELECT LENGTH(?) AS res", value = 5)
    test("octetLength") - checker(
      query = Db("叉烧包").octetLength,
      sql = "SELECT OCTET_LENGTH(?) AS res",
      value = 9,
      moreValues = Seq(6) // Not sure why HsqlDb returns different value here ???
    )

    test("position") - checker(
      query = Db("hello").indexOf("ll"),
      sqls = Seq("SELECT POSITION(? IN ?) AS res", "SELECT INSTR(?, ?) AS res"),
      value = 3
    )

    test("toLowerCase") -
      checker(query = Db("Hello").toLowerCase, sql = "SELECT LOWER(?) AS res", value = "hello")

    test("trim") -
      checker(query = Db("  Hello ").trim, sql = "SELECT TRIM(?) AS res", value = "Hello")

    test("ltrim") -
      checker(query = Db("  Hello ").ltrim, sql = "SELECT LTRIM(?) AS res", value = "Hello ")

    test("rtrim") -
      checker(query = Db("  Hello ").rtrim, sql = "SELECT RTRIM(?) AS res", value = "  Hello")

    test("substring") - checker(
      query = Db("Hello").substring(2, 2),
      sql = "SELECT SUBSTRING(?, ?, ?) AS res",
      value = "el"
    )
    test("startsWith") - checker(
      query = Db("Hello").startsWith("Hel"),
      sqls = Seq(
        "SELECT (? LIKE ? || '%') AS res",
        "SELECT (? LIKE CONCAT(?, '%')) AS res"
      ),
      value = true
    )
    test("endsWith") - checker(
      query = Db("Hello").endsWith("llo"),
      sqls = Seq(
        "SELECT (? LIKE '%' || ?) AS res",
        "SELECT (? LIKE CONCAT('%', ?)) AS res"
      ),
      value = true
    )
    test("contains") - checker(
      query = Db("Hello").contains("ll"),
      sqls = Seq(
        "SELECT (? LIKE '%' || ? || '%') AS res",
        "SELECT (? LIKE CONCAT('%', ?, '%')) AS res"
      ),
      value = true
    )
    test("replace") - checker(
      query = Db("Hello").replace("ll", "rr"),
      sqls = Seq(
        "SELECT REPLACE(?, ?, ?) AS res"
      ),
      value = "Herro"
    )

//    test("overlay") - checker(
//      query = Db("Hello").overlay("LL", 2, 2),
//      sql = "SELECT OVERLAY(? PLACING ? FROM ? FOR ?) AS res",
//      value = "HeLLo"
//    )
  }
}
