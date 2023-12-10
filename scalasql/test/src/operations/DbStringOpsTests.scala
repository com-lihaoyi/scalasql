package scalasql.operations

import scalasql._
import scalasql.core.Expr
import utest._
import utils.ScalaSqlSuite

trait ExprStringOpsTests extends ScalaSqlSuite {
  def description = "Operations that can be performed on `Expr[String]`"
  def tests = Tests {
    test("plus") - checker(
      query = Expr("hello") + Expr("world"),
      sqls = Seq("SELECT (? || ?) AS res", "SELECT CONCAT(?, ?) AS res"),
      value = "helloworld"
    )

    test("like") - checker(
      query = Expr("hello").like("he%"),
      sql = "SELECT (? LIKE ?) AS res",
      value = true
    )

    test("length") - checker(
      query = Expr("hello").length,
      sql = "SELECT LENGTH(?) AS res",
      value = 5
    )

    test("octetLength") - checker(
      query = Expr("叉烧包").octetLength,
      sql = "SELECT OCTET_LENGTH(?) AS res",
      value = 9,
      moreValues = Seq(6) // Not sure why HsqlExpr returns different value here ???
    )

    test("position") - checker(
      query = Expr("hello").indexOf("ll"),
      sqls = Seq("SELECT POSITION(? IN ?) AS res", "SELECT INSTR(?, ?) AS res"),
      value = 3
    )

    test("toLowerCase") - checker(
      query = Expr("Hello").toLowerCase,
      sql = "SELECT LOWER(?) AS res",
      value = "hello"
    )

    test("trim") - checker(
      query = Expr("  Hello ").trim,
      sql = "SELECT TRIM(?) AS res",
      value = "Hello"
    )

    test("ltrim") - checker(
      query = Expr("  Hello ").ltrim,
      sql = "SELECT LTRIM(?) AS res",
      value = "Hello "
    )

    test("rtrim") - checker(
      query = Expr("  Hello ").rtrim,
      sql = "SELECT RTRIM(?) AS res",
      value = "  Hello"
    )

    test("substring") - checker(
      query = Expr("Hello").substring(2, 2),
      sql = "SELECT SUBSTRING(?, ?, ?) AS res",
      value = "el"
    )

    test("startsWith") - checker(
      query = Expr("Hello").startsWith("Hel"),
      sqls = Seq(
        "SELECT (? LIKE ? || '%') AS res",
        "SELECT (? LIKE CONCAT(?, '%')) AS res"
      ),
      value = true
    )

    test("endsWith") - checker(
      query = Expr("Hello").endsWith("llo"),
      sqls = Seq(
        "SELECT (? LIKE '%' || ?) AS res",
        "SELECT (? LIKE CONCAT('%', ?)) AS res"
      ),
      value = true
    )

    test("contains") - checker(
      query = Expr("Hello").contains("ll"),
      sqls = Seq(
        "SELECT (? LIKE '%' || ? || '%') AS res",
        "SELECT (? LIKE CONCAT('%', ?, '%')) AS res"
      ),
      value = true
    )

    test("replace") - checker(
      query = Expr("Hello").replace("ll", "rr"),
      sqls = Seq(
        "SELECT REPLACE(?, ?, ?) AS res"
      ),
      value = "Herro"
    )
  }
}
