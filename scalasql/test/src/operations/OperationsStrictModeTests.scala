package scalasql.operations

import utest.{*, given}
import scalasql.Expr
import scalasql.dialects.Dialect
import scalasql.dialects.SqliteDialect

object OperationsStrictModeTests extends TestSuite with SqliteDialect {
  import scalasql.operations.strict.given

  val tests = Tests {
    test("check") {
      // val a = Expr(1) `=` Expr(false)
      assertCompileError("Expr(1) `=` Expr(false)")
    }
  }
}
