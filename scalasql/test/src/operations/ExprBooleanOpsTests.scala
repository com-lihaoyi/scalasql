package scalasql.operations

import scalasql._
import scalasql.query.Expr
import utest._
import utils.ScalaSqlSuite

trait ExprBooleanOpsTests extends ScalaSqlSuite {
  def description = "Operations that can be performed on `Expr[Boolean]`"
  def tests = Tests {
    test("and") {
      checker(query = Expr(true) && Expr(true), sql = "SELECT ? AND ? as res", value = true)
      checker(query = Expr(false) && Expr(true), sql = "SELECT ? AND ? as res", value = false)
    }

    test("or") {
      checker(query = Expr(false) || Expr(false), sql = "SELECT ? OR ? as res", value = false)
      checker(query = !Expr(false), sql = "SELECT NOT ? as res", value = true)
    }
  }
}
