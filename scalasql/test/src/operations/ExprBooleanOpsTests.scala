package scalasql.operations

import scalasql._
import scalasql.query.Sql
import utest._
import utils.ScalaSqlSuite

trait ExprBooleanOpsTests extends ScalaSqlSuite {
  def description = "Operations that can be performed on `Sql[Boolean]`"
  def tests = Tests {
    test("and") {
      checker(query = Sql(true) && Sql(true), sql = "SELECT (? AND ?) AS res", value = true)
      checker(query = Sql(false) && Sql(true), sql = "SELECT (? AND ?) AS res", value = false)
    }

    test("or") {
      checker(query = Sql(false) || Sql(false), sql = "SELECT (? OR ?) AS res", value = false)
      checker(query = !Sql(false), sql = "SELECT (NOT ?) AS res", value = true)
    }
  }
}
