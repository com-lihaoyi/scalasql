package scalasql.operations

import scalasql._
import scalasql.query.Expr
import utest._
import utils.ScalaSqlSuite

trait ExprOpsTests extends ScalaSqlSuite {
  def description = "Operations that can be performed on `Expr[T]` for any `T`"
  def tests = Tests {

    test("numeric") {
      test("greaterThan") -
        checker(query = Expr(6) > Expr(2), sql = "SELECT ? > ? as res", value = true)

      test("lessThan") -
        checker(query = Expr(6) < Expr(2), sql = "SELECT ? < ? as res", value = false)

      test("greaterThanOrEquals") -
        checker(query = Expr(6) >= Expr(2), sql = "SELECT ? >= ? as res", value = true)

      test("lessThanOrEquals") -
        checker(query = Expr(6) <= Expr(2), sql = "SELECT ? <= ? as res", value = false)
    }

    test("string") {
      test("greaterThan") -
        checker(query = Expr("A") > Expr("B"), sql = "SELECT ? > ? as res", value = false)

      test("lessThan") -
        checker(query = Expr("A") < Expr("B"), sql = "SELECT ? < ? as res", value = true)

      test("greaterThanOrEquals") -
        checker(query = Expr("A") >= Expr("B"), sql = "SELECT ? >= ? as res", value = false)

      test("lessThanOrEquals") -
        checker(query = Expr("A") <= Expr("B"), sql = "SELECT ? <= ? as res", value = true)
    }

    test("boolean") {
      test("greaterThan") -
        checker(query = Expr(true) > Expr(false), sql = "SELECT ? > ? as res", value = true)

      test("lessThan") -
        checker(query = Expr(true) < Expr(true), sql = "SELECT ? < ? as res", value = false)

      test("greaterThanOrEquals") -
        checker(query = Expr(true) >= Expr(true), sql = "SELECT ? >= ? as res", value = true)

      test("lessThanOrEquals") -
        checker(query = Expr(true) <= Expr(true), sql = "SELECT ? <= ? as res", value = true)
    }

    test("cast"){
      test("byte") - checker(
        query = Expr(45.12).cast[Byte],
        sqls = Seq(
          "SELECT CAST(? AS TINYINT) as res",
          "SELECT CAST(? AS INTEGER) as res",
          "SELECT CAST(? AS SIGNED) as res"
        ),
        value = 45: Byte
      )

      test("short") - checker(
        query = Expr(1234.1234).cast[Short],
        sqls = Seq(
          "SELECT CAST(? AS SMALLINT) as res",
          "SELECT CAST(? AS SIGNED) as res"
        ),
        value = 1234: Short
      )

      test("int") - checker(
        query = Expr(1234.1234).cast[Int],
        sqls = Seq(
          "SELECT CAST(? AS INTEGER) as res",
          "SELECT CAST(? AS SIGNED) as res"
        ),
        value = 1234
      )

      test("long") - checker(
        query = Expr(1234.1234).cast[Long],
        sqls = Seq(
          "SELECT CAST(? AS BIGINT) as res",
          "SELECT CAST(? AS SIGNED) as res"
        ),
        value = 1234l
      )

      test("string") - checker(
        query = Expr(1234.5678).cast[String],
        sqls = Seq(
          "SELECT CAST(? AS LONGVARCHAR) as res",
          "SELECT CAST(? AS VARCHAR) as res",
          "SELECT CAST(? AS CHAR) as res"
        ),
        value = "1234.5678"
      )
    }
  }
}
