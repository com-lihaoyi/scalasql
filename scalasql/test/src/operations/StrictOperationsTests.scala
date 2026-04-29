package scalasql.operations

import utest.{*, given}
import scalasql.Expr
import scalasql.dialects.Dialect
import scalasql.dialects.SqliteDialect

object StrictOperationsTests extends TestSuite with SqliteDialect {

  val tests = Tests {
    test("crossTypeMismatch") {
      test("intVsBoolean") { assertCompileError("Expr(1) `=` Expr(false)") }
      test("stringVsInt") { assertCompileError("Expr(\"a\") `=` Expr(1)") }
      test("booleanVsInt") { assertCompileError("Expr(true) `=` Expr(1)") }
    }

    test("optionWithDifferentBaseTypes") {
      test("optionIntVsString") { assertCompileError("Expr(Option(1)) `=` Expr(\"a\")") }
      test("optionBooleanVsInt") { assertCompileError("Expr(1) `=` Expr(Option(true))") }
      test("optionBooleanVsInt") { assertCompileError("Expr(Option(true)) `=` Expr(Option(1))") }
    }

    test("sameTypeComparison") {
      test("intVsInt") { Expr(1) `=` Expr(2) }
      test("stringVsString") { Expr("a") `=` Expr("b") }
      test("booleanVsBoolean") { Expr(true) `=` Expr(false) }
    }

    test("nullableCompatibility") {
      test("optionIntVsInt") { Expr(Option(1)) `=` Expr(2) }
      test("intVsOptionInt") { Expr(1) `=` Expr(Option(2)) }
      test("optionIntVsOptionInt") { Expr(Option(1)) `=` Expr(Option(1)) }
    }
  }
}
