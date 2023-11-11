package operations

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
        checker(query = Expr(true) <= Expr(true), sql = "SELECT ? <= ? as res", value = false)
    }
  }
}
