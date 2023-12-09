package scalasql.operations

import scalasql._
import scalasql.core.Db
import utest._
import utils.ScalaSqlSuite

trait DbBooleanOpsTests extends ScalaSqlSuite {
  def description = "Operations that can be performed on `Db[Boolean]`"
  def tests = Tests {
    test("and") {
      checker(query = Db(true) && Db(true), sql = "SELECT (? AND ?) AS res", value = true)
      checker(query = Db(false) && Db(true), sql = "SELECT (? AND ?) AS res", value = false)
    }

    test("or") {
      checker(query = Db(false) || Db(false), sql = "SELECT (? OR ?) AS res", value = false)
      checker(query = !Db(false), sql = "SELECT (NOT ?) AS res", value = true)
    }
  }
}
