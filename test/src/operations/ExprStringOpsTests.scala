package usql.operations

import usql.ExprOps._
import usql._
import usql.query.Expr
import utest._

object SqliteExprStringOpsTests extends ExprStringOpsTests with SqliteSuite
object PgExprExprStringOpsTests extends ExprStringOpsTests with PostgresSuite

/**
 * Tests for all the individual symbolic operators and functions that we provide by default
 */
trait ExprStringOpsTests extends TestSuite  {
  val checker: TestDb
  def tests = Tests {
    test("like") - checker(
      query = Expr("hello").like("he%"),
      sql = "SELECT ? LIKE ? as res",
      value = true
    )

    test("position") - checker(
      query = Expr("ll").position("hello"),
      sql = "SELECT POSITION(? IN ?) as res",
      value = 3
    )

    test("toLowerCase") - checker(
      query = Expr("Hello").toLowerCase,
      sql = "SELECT LOWER(?) as res",
      value = "hello"
    )

    test("trim") - checker(
      query = Expr("  Hello ").trim,
      sql = "SELECT TRIM(?) as res",
      value = "Hello"
    )

    test("substring") - checker(
      query = Expr("Hello").substring(2, 2),
      sql = "SELECT SUBSTRING(?, ?, ?) as res",
      value = "el"
    )

//    test("overlay") - checker(
//      query = Expr("Hello").overlay("LL", 2, 2),
//      sql = "SELECT OVERLAY(? PLACING ? FROM ? FOR ?) as res",
//      value = "HeLLo"
//    )
  }
}
