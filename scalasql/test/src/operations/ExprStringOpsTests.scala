package scalasql.operations

import scalasql._
import scalasql.query.Sql
import utest._
import utils.ScalaSqlSuite

trait ExprStringOpsTests extends ScalaSqlSuite {
  def description = "Operations that can be performed on `Sql[String]`"
  def tests = Tests {
    test("plus") - checker(
      query = Sql("hello") + Sql("world"),
      sqls = Seq("SELECT (? || ?) AS res", "SELECT CONCAT(?, ?) AS res"),
      value = "helloworld"
    )
    test("like") -
      checker(query = Sql("hello").like("he%"), sql = "SELECT (? LIKE ?) AS res", value = true)
    test("length") -
      checker(query = Sql("hello").length, sql = "SELECT LENGTH(?) AS res", value = 5)
    test("octetLength") - checker(
      query = Sql("叉烧包").octetLength,
      sql = "SELECT OCTET_LENGTH(?) AS res",
      value = 9,
      moreValues = Seq(6) // Not sure why HsqlDb returns different value here ???
    )

    test("position") - checker(
      query = Sql("hello").indexOf("ll"),
      sqls = Seq("SELECT POSITION(? IN ?) AS res", "SELECT INSTR(?, ?) AS res"),
      value = 3
    )

    test("toLowerCase") -
      checker(query = Sql("Hello").toLowerCase, sql = "SELECT LOWER(?) AS res", value = "hello")

    test("trim") -
      checker(query = Sql("  Hello ").trim, sql = "SELECT TRIM(?) AS res", value = "Hello")

    test("ltrim") -
      checker(query = Sql("  Hello ").ltrim, sql = "SELECT LTRIM(?) AS res", value = "Hello ")

    test("rtrim") -
      checker(query = Sql("  Hello ").rtrim, sql = "SELECT RTRIM(?) AS res", value = "  Hello")

    test("substring") - checker(
      query = Sql("Hello").substring(2, 2),
      sql = "SELECT SUBSTRING(?, ?, ?) AS res",
      value = "el"
    )
    test("startsWith") - checker(
      query = Sql("Hello").startsWith("Hel"),
      sqls = Seq(
        "SELECT (? LIKE ? || '%') AS res",
        "SELECT (? LIKE CONCAT(?, '%')) AS res"
      ),
      value = true
    )
    test("endsWith") - checker(
      query = Sql("Hello").endsWith("llo"),
      sqls = Seq(
        "SELECT (? LIKE '%' || ?) AS res",
        "SELECT (? LIKE CONCAT('%', ?)) AS res"
      ),
      value = true
    )
    test("contains") - checker(
      query = Sql("Hello").contains("ll"),
      sqls = Seq(
        "SELECT (? LIKE '%' || ? || '%') AS res",
        "SELECT (? LIKE CONCAT('%', ?, '%')) AS res"
      ),
      value = true
    )

//    test("overlay") - checker(
//      query = Sql("Hello").overlay("LL", 2, 2),
//      sql = "SELECT OVERLAY(? PLACING ? FROM ? FOR ?) AS res",
//      value = "HeLLo"
//    )
  }
}
