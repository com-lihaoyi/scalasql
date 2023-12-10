package scalasql.operations

import scalasql._
import scalasql.core.SqlStr.SqlStringSyntax
import scalasql.core.Expr
import utest._
import utils.ScalaSqlSuite

trait ExprOpsTests extends ScalaSqlSuite {
  def description = "Operations that can be performed on `Expr[T]` for any `T`"
  def tests = Tests {

    test("numeric") {
      test("greaterThan") -
        checker(query = Expr(6) > Expr(2), sql = "SELECT (? > ?) AS res", value = true)

      test("lessThan") -
        checker(query = Expr(6) < Expr(2), sql = "SELECT (? < ?) AS res", value = false)

      test("greaterThanOrEquals") -
        checker(query = Expr(6) >= Expr(2), sql = "SELECT (? >= ?) AS res", value = true)

      test("lessThanOrEquals") -
        checker(query = Expr(6) <= Expr(2), sql = "SELECT (? <= ?) AS res", value = false)
    }

    test("string") {
      test("greaterThan") -
        checker(query = Expr("A") > Expr("B"), sql = "SELECT (? > ?) AS res", value = false)

      test("lessThan") -
        checker(query = Expr("A") < Expr("B"), sql = "SELECT (? < ?) AS res", value = true)

      test("greaterThanOrEquals") -
        checker(query = Expr("A") >= Expr("B"), sql = "SELECT (? >= ?) AS res", value = false)

      test("lessThanOrEquals") -
        checker(query = Expr("A") <= Expr("B"), sql = "SELECT (? <= ?) AS res", value = true)
    }

    test("boolean") {
      test("greaterThan") -
        checker(query = Expr(true) > Expr(false), sql = "SELECT (? > ?) AS res", value = true)

      test("lessThan") -
        checker(query = Expr(true) < Expr(true), sql = "SELECT (? < ?) AS res", value = false)

      test("greaterThanOrEquals") -
        checker(query = Expr(true) >= Expr(true), sql = "SELECT (? >= ?) AS res", value = true)

      test("lessThanOrEquals") -
        checker(query = Expr(true) <= Expr(true), sql = "SELECT (? <= ?) AS res", value = true)
    }

    test("cast") {
      test("byte") - checker(
        query = Expr(45.12).cast[Byte],
        sqls = Seq(
          "SELECT CAST(? AS TINYINT) AS res",
          "SELECT CAST(? AS INTEGER) AS res",
          "SELECT CAST(? AS SIGNED) AS res"
        ),
        value = 45: Byte
      )

      test("short") - checker(
        query = Expr(1234.1234).cast[Short],
        sqls = Seq(
          "SELECT CAST(? AS SMALLINT) AS res",
          "SELECT CAST(? AS SIGNED) AS res"
        ),
        value = 1234: Short
      )

      test("int") - checker(
        query = Expr(1234.1234).cast[Int],
        sqls = Seq(
          "SELECT CAST(? AS INTEGER) AS res",
          "SELECT CAST(? AS SIGNED) AS res"
        ),
        value = 1234
      )

      test("long") - checker(
        query = Expr(1234.1234).cast[Long],
        sqls = Seq(
          "SELECT CAST(? AS BIGINT) AS res",
          "SELECT CAST(? AS SIGNED) AS res"
        ),
        value = 1234L
      )

      test("string") - checker(
        query = Expr(1234.5678).cast[String],
        sqls = Seq(
          "SELECT CAST(? AS LONGVARCHAR) AS res",
          "SELECT CAST(? AS VARCHAR) AS res",
          "SELECT CAST(? AS CHAR) AS res"
        ),
        value = "1234.5678"
      )

      test("localdate") - checker(
        query = Expr("2001-02-03").cast[java.time.LocalDate],
        sqls = Seq(
          "SELECT CAST(? AS DATE) AS res",
          "SELECT CAST(? AS VARCHAR) AS res"
        ),
        value = java.time.LocalDate.parse("2001-02-03")
      )

      test("localdatetime") - checker(
        query = Expr("2023-11-12 03:22:41").cast[java.time.LocalDateTime],
        sqls = Seq(
          "SELECT CAST(? AS DATETIME) AS res",
          "SELECT CAST(? AS TIMESTAMP) AS res",
          "SELECT CAST(? AS VARCHAR) AS res"
        ),
        value = java.time.LocalDateTime.parse("2023-11-12T03:22:41")
      )

      test("instant") - checker(
        query = Expr("2007-12-03 10:15:30.00").cast[java.time.Instant],
        sqls = Seq(
          "SELECT CAST(? AS DATETIME) AS res",
          "SELECT CAST(? AS TIMESTAMP) AS res",
          "SELECT CAST(? AS VARCHAR) AS res"
        ),
        value = java.time.Instant.parse("2007-12-03T02:15:30.00Z")
      )

      test("castNamed") - checker(
        query = Expr(1234.5678).castNamed[String](sql"CHAR(3)"),
        sql = "SELECT CAST(? AS CHAR(3)) AS res",
        value = "123",
        moreValues = Seq("1234.5678") // SQLITE doesn't truncate on cast
      )
    }
  }
}
