package scalasql.operations

import scalasql._
import scalasql.core.Sql
import utest._
import utils.ScalaSqlSuite

trait ExprNumericOpsTests extends ScalaSqlSuite {
  def description = "Operations that can be performed on `Sql[T]` when `T` is numeric"
  def tests = Tests {
    test("plus") - checker(query = Sql(6) + Sql(2), sql = "SELECT (? + ?) AS res", value = 8)

    test("minus") - checker(query = Sql(6) - Sql(2), sql = "SELECT (? - ?) AS res", value = 4)

    test("times") - checker(query = Sql(6) * Sql(2), sql = "SELECT (? * ?) AS res", value = 12)

    test("divide") - checker(query = Sql(6) / Sql(2), sql = "SELECT (? / ?) AS res", value = 3)

    test("modulo") - checker(query = Sql(6) % Sql(2), sql = "SELECT MOD(?, ?) AS res", value = 0)

    test("bitwiseAnd") - checker(
      query = Sql(6) & Sql(2),
      sqls = Seq("SELECT (? & ?) AS res", "SELECT BITAND(?, ?) AS res"),
      value = 2
    )

    test("bitwiseOr") - checker(
      query = Sql(6) | Sql(3),
      sqls = Seq("SELECT (? | ?) AS res", "SELECT BITOR(?, ?) AS res"),
      value = 7
    )

    test("between") - checker(
      query = Sql(4).between(Sql(2), Sql(6)),
      sql = "SELECT ? BETWEEN ? AND ? AS res",
      value = true
    )

    test("unaryPlus") - checker(query = +Sql(-4), sql = "SELECT +? AS res", value = -4)

    test("unaryMinus") -
      checker(query = -Sql(-4), sqls = Seq("SELECT -? AS res", "SELECT -(?) AS res"), value = 4)

    test("unaryTilde") - checker(
      query = ~Sql(-4),
      sqls = Seq("SELECT ~? AS res", "SELECT BITNOT(?) AS res"),
      value = 3
    )

    test("abs") - checker(query = Sql(-4).abs, sql = "SELECT ABS(?) AS res", value = 4)

    test("mod") - checker(query = Sql(8).mod(Sql(3)), sql = "SELECT MOD(?, ?) AS res", value = 2)

    test("ceil") - checker(query = Sql(4.3).ceil, sql = "SELECT CEIL(?) AS res", value = 5.0)

    test("floor") - checker(query = Sql(4.7).floor, sql = "SELECT FLOOR(?) AS res", value = 4.0)

    test("floor") - checker(query = Sql(4.7).floor, sql = "SELECT FLOOR(?) AS res", value = 4.0)

    test("precedence") - checker(
      query = (Sql(2) + Sql(3)) * Sql(4),
      sql = "SELECT ((? + ?) * ?) AS res",
      value = 20
    )
  }
}
