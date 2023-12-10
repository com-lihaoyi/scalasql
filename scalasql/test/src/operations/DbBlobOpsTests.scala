package scalasql.operations

import scalasql.{Expr, Bytes}
import scalasql.utils.ScalaSqlSuite
import utest._

trait ExprBlobOpsTests extends ScalaSqlSuite {
  def description = "Operations that can be performed on `Expr[Bytes]`"

  def tests = Tests {
    test("plus") - checker(
      query = Expr(Bytes("hello")) + Expr(Bytes("world")),
      sqls = Seq("SELECT (? || ?) AS res", "SELECT CONCAT(?, ?) AS res"),
      value = Bytes("helloworld")
    )

    test("like") - checker(
      query = Expr(Bytes("hello")).like(Bytes("he%")),
      sql = "SELECT (? LIKE ?) AS res",
      value = true
    )

    test("length") - checker(
      query = Expr(Bytes("hello")).length,
      sql = "SELECT LENGTH(?) AS res",
      value = 5
    )

    test("octetLength") - checker(
      query = Expr(Bytes("叉烧包")).octetLength,
      sql = "SELECT OCTET_LENGTH(?) AS res",
      value = 9,
      moreValues = Seq(6) // Not sure why HsqlExpr returns different value here ???
    )

    test("position") - checker(
      query = Expr(Bytes("hello")).indexOf(Bytes("ll")),
      sqls = Seq("SELECT POSITION(? IN ?) AS res", "SELECT INSTR(?, ?) AS res"),
      value = 3
    )
    // Not supported by postgres
//
//    test("toLowerCase") -
//      checker(query = Expr(Bytes("Hello").toLowerCase, sql = "SELECT LOWER(?) AS res", value = Bytes("hello"))
//
//    test("trim") -
//      checker(query = Expr(Bytes("  Hello ").trim, sql = "SELECT TRIM(?) AS res", value = Bytes("Hello"))
//
//    test("ltrim") -
//      checker(query = Expr(Bytes("  Hello ").ltrim, sql = "SELECT LTRIM(?) AS res", value = Bytes("Hello "))
//
//    test("rtrim") -
//      checker(query = Expr(Bytes("  Hello ").rtrim, sql = "SELECT RTRIM(?) AS res", value = Bytes("  Hello"))

    test("substring") - checker(
      query = Expr(Bytes("Hello")).substring(2, 2),
      sql = "SELECT SUBSTRING(?, ?, ?) AS res",
      value = Bytes("el")
    )

    test("startsWith") - checker(
      query = Expr(Bytes("Hello")).startsWith(Bytes("Hel")),
      sqls = Seq(
        "SELECT (? LIKE ? || '%') AS res",
        "SELECT (? LIKE CONCAT(?, '%')) AS res"
      ),
      value = true
    )

    test("endsWith") - checker(
      query = Expr(Bytes("Hello")).endsWith(Bytes("llo")),
      sqls = Seq(
        "SELECT (? LIKE '%' || ?) AS res",
        "SELECT (? LIKE CONCAT('%', ?)) AS res"
      ),
      value = true
    )

    test("contains") - checker(
      query = Expr(Bytes("Hello")).contains(Bytes("ll")),
      sqls = Seq(
        "SELECT (? LIKE '%' || ? || '%') AS res",
        "SELECT (? LIKE CONCAT('%', ?, '%')) AS res"
      ),
      value = true
    )
    // Not supported by postgres
//    test("replace") - checker(
//      query = Expr(Bytes("Hello").replace(Bytes("ll"), Bytes("rr")),
//      sqls = Seq(
//        "SELECT REPLACE(?, ?, ?) AS res"
//      ),
//      value = Bytes("Herro")
//    )
  }
}
