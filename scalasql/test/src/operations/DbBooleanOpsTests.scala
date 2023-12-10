package scalasql.operations

import scalasql._
import scalasql.core.Expr
import utest._
import utils.ScalaSqlSuite

trait ExprBooleanOpsTests extends ScalaSqlSuite {
  def description = "Operations that can be performed on `Expr[Boolean]`"
  def tests = Tests {
    test("and") {
      checker(query = Expr(true) && Expr(true), sql = "SELECT (? AND ?) AS res", value = true)
      checker(query = Expr(false) && Expr(true), sql = "SELECT (? AND ?) AS res", value = false)
    }

    test("or") {
      checker(query = Expr(false) || Expr(false), sql = "SELECT (? OR ?) AS res", value = false)
      checker(query = !Expr(false), sql = "SELECT (NOT ?) AS res", value = true)
    }
  }
}
