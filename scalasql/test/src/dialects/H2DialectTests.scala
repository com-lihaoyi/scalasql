package scalasql.dialects

import scalasql.core.Db
import scalasql.utils.H2Suite
import utest._

trait H2DialectTests extends H2Suite {
  def description = "Operations specific to working with H2 Databases"
  def tests = Tests {

    test("ltrim2") - checker(
      query = Db("xxHellox").ltrim("x"),
      sql = "SELECT LTRIM(?, ?) AS res",
      value = "Hellox"
    )

    test("rtrim2") - checker(
      query = Db("xxHellox").rtrim("x"),
      sql = "SELECT RTRIM(?, ?) AS res",
      value = "xxHello"
    )

    test("lpad") - checker(
      query = Db("Hello").lpad(10, "xy"),
      sql = "SELECT LPAD(?, ?, ?) AS res",
      value = "xxxxxHello" // H2 only uses first character of fill string
    )

    test("rpad") - checker(
      query = Db("Hello").rpad(10, "xy"),
      sql = "SELECT RPAD(?, ?, ?) AS res",
      value = "Helloxxxxx" // H2 only uses first character of fill string
    )

    test("concat") - checker(
      query = db.concat("i ", "am", " cow", 1337),
      sql = "SELECT CONCAT(?, ?, ?, ?) AS res",
      value = "i am cow1337"
    )

    test("concatWs") - checker(
      query = db.concatWs(" ", "i", "am", "cow", 1337),
      sql = "SELECT CONCAT_WS(?, ?, ?, ?, ?) AS res",
      value = "i am cow 1337"
    )
  }
}
