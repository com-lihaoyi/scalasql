package scalasql.operations

import scalasql._
import scalasql.core.Db
import utest._
import utils.ScalaSqlSuite

trait DbNumericOpsTests extends ScalaSqlSuite {
  def description = "Operations that can be performed on `Db[T]` when `T` is numeric"
  def tests = Tests {
    test("plus") - checker(query = Db(6) + Db(2), sql = "SELECT (? + ?) AS res", value = 8)

    test("minus") - checker(query = Db(6) - Db(2), sql = "SELECT (? - ?) AS res", value = 4)

    test("times") - checker(query = Db(6) * Db(2), sql = "SELECT (? * ?) AS res", value = 12)

    test("divide") - checker(query = Db(6) / Db(2), sql = "SELECT (? / ?) AS res", value = 3)

    test("modulo") - checker(query = Db(6) % Db(2), sql = "SELECT MOD(?, ?) AS res", value = 0)

    test("bitwiseAnd") - checker(
      query = Db(6) & Db(2),
      sqls = Seq("SELECT (? & ?) AS res", "SELECT BITAND(?, ?) AS res"),
      value = 2
    )

    test("bitwiseOr") - checker(
      query = Db(6) | Db(3),
      sqls = Seq("SELECT (? | ?) AS res", "SELECT BITOR(?, ?) AS res"),
      value = 7
    )

    test("between") - checker(
      query = Db(4).between(Db(2), Db(6)),
      sql = "SELECT ? BETWEEN ? AND ? AS res",
      value = true
    )

    test("unaryPlus") - checker(query = +Db(-4), sql = "SELECT +? AS res", value = -4)

    test("unaryMinus") -
      checker(query = -Db(-4), sqls = Seq("SELECT -? AS res", "SELECT -(?) AS res"), value = 4)

    test("unaryTilde") - checker(
      query = ~Db(-4),
      sqls = Seq("SELECT ~? AS res", "SELECT BITNOT(?) AS res"),
      value = 3
    )

    test("abs") - checker(query = Db(-4).abs, sql = "SELECT ABS(?) AS res", value = 4)

    test("mod") - checker(query = Db(8).mod(Db(3)), sql = "SELECT MOD(?, ?) AS res", value = 2)

    test("ceil") - checker(query = Db(4.3).ceil, sql = "SELECT CEIL(?) AS res", value = 5.0)

    test("floor") - checker(query = Db(4.7).floor, sql = "SELECT FLOOR(?) AS res", value = 4.0)

    test("floor") - checker(query = Db(4.7).floor, sql = "SELECT FLOOR(?) AS res", value = 4.0)

    test("precedence") - checker(
      query = (Db(2) + Db(3)) * Db(4),
      sql = "SELECT ((? + ?) * ?) AS res",
      value = 20
    )
  }
}
