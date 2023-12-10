package scalasql.operations

import scalasql._
import utest._
import utils.ScalaSqlSuite

trait ExprAggNumericOpsTests extends ScalaSqlSuite {
  def description = "Operations that can be performed on `Expr[Seq[T]]` where `T` is numeric"
  def tests = Tests {
    test("sum") - checker(
      query = Purchase.select.map(_.count).sum,
      sql = "SELECT SUM(purchase0.count) AS res FROM purchase purchase0",
      value = 140
    )

    test("min") - checker(
      query = Purchase.select.map(_.count).min,
      sql = "SELECT MIN(purchase0.count) AS res FROM purchase purchase0",
      value = 3
    )

    test("max") - checker(
      query = Purchase.select.map(_.count).max,
      sql = "SELECT MAX(purchase0.count) AS res FROM purchase purchase0",
      value = 100
    )

    test("avg") - checker(
      query = Purchase.select.map(_.count).avg,
      sql = "SELECT AVG(purchase0.count) AS res FROM purchase purchase0",
      value = 20
    )
  }
}
