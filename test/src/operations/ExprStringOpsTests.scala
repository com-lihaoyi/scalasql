package scalasql.operations

import scalasql._
import scalasql.query.Expr
import utest._

/**
 * Tests for all the individual symbolic operators and functions that we provide by default
 */
trait ExprStringOpsTests extends ScalaSqlSuite {
  def tests = Tests {
    test("plus") - checker(
      query = Expr("hello") + Expr("world"),
      sqls = Seq(
        "SELECT ? || ? as res",
        "SELECT CONCAT(?, ?) as res"
      ),
      value = "helloworld"
    )
    test("like") - checker(
      query = Expr("hello").like("he%"),
      sql = "SELECT ? LIKE ? as res",
      value = true
    )
    test("length") - checker(
      query = Expr("hello").length,
      sql = "SELECT LENGTH(?) as res",
      value = 5
    )
    test("octetLength") - checker(
      query = Expr("叉烧包").octetLength,
      sql = "SELECT OCTET_LENGTH(?) as res",
      value = 9,
      moreValues = Seq(6) // Not sure why HsqlDb returns different value here ???
    )

    test("position") - checker(
      query = Expr("hello").indexOf("ll"),
      sqls = Seq(
        "SELECT POSITION(? IN ?) as res",
        "SELECT INSTR(?, ?) as res"
      ),
      value = 3
    )

    test("toLowerCase") - checker(
      query = Expr("Hello").toLowerCase,
      sql = "SELECT LOWER(?) as res",
      value = "hello"
    )

    test("trim") - checker(
      query = Expr("  Hello ").trim,
      sql = "SELECT TRIM(?) as res",
      value = "Hello"
    )

    test("ltrim") - checker(
      query = Expr("  Hello ").ltrim,
      sql = "SELECT LTRIM(?) as res",
      value = "Hello "
    )

    test("rtrim") - checker(
      query = Expr("  Hello ").rtrim,
      sql = "SELECT RTRIM(?) as res",
      value = "  Hello"
    )

    test("substring") - checker(
      query = Expr("Hello").substring(2, 2),
      sql = "SELECT SUBSTRING(?, ?, ?) as res",
      value = "el"
    )

//    test("overlay") - checker(
//      query = Expr("Hello").overlay("LL", 2, 2),
//      sql = "SELECT OVERLAY(? PLACING ? FROM ? FOR ?) as res",
//      value = "HeLLo"
//    )
  }
}
