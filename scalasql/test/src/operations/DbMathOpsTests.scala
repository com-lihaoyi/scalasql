package scalasql.operations

import scalasql.core.DbApi
import scalasql.utils.ScalaSqlSuite
import utest._

trait ExprMathOpsTests extends ScalaSqlSuite {
  override implicit def DbApiOpsConv(db: => DbApi): DbApiOps & MathOps = ???
  def description = "Math operations; supported by H2/Postgres/MySql, not supported by Sqlite"
  def tests = Tests {

    test("power") - checker(
      query = db.power(10, 3),
      sql = "SELECT POWER(?, ?) AS res",
      value = 1000.0
    )

    test("sqrt") - checker(
      query = db.sqrt(9),
      sql = "SELECT SQRT(?) AS res",
      value = 3.0
    )

    test("ln") - checker(
      query = db.ln(16.0),
      sql = "SELECT LN(?) AS res"
    )

    test("log") - checker(
      query = db.log(2, 8),
      sql = "SELECT LOG(?, ?) AS res"
    )

    test("log10") - checker(
      query = db.log10(16.0),
      sql = "SELECT LOG10(?) AS res"
    )

    test("exp") - checker(
      query = db.exp(16.0),
      sql = "SELECT EXP(?) AS res"
    )

    test("sin") - checker(
      query = db.sin(16.0),
      sql = "SELECT SIN(?) AS res"
    )

    test("cos") - checker(
      query = db.cos(16.0),
      sql = "SELECT COS(?) AS res"
    )

    test("tan") - checker(
      query = db.tan(16.0),
      sql = "SELECT TAN(?) AS res"
    )

    test("asin") - checker(
      query = db.asin(1.0),
      sql = "SELECT ASIN(?) AS res"
    )

    test("acos") - checker(
      query = db.acos(1.0),
      sql = "SELECT ACOS(?) AS res"
    )

    test("atan") - checker(
      query = db.atan(1.0),
      sql = "SELECT ATAN(?) AS res"
    )

    test("atan2") - checker(
      query = db.atan2(16.0, 23.0),
      sql = "SELECT ATAN2(?, ?) AS res"
    )

    test("pi") - checker(
      query = db.pi,
      sql = "SELECT PI() AS res"
    )

    test("degrees") - checker(
      query = db.degrees(180),
      sql = "SELECT DEGREES(?) AS res"
    )

    test("radians") - checker(
      query = db.radians(180),
      sql = "SELECT RADIANS(?) AS res"
    )
  }
}
