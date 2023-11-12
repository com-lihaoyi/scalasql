package scalasql
import utest._

/**
 * Make sure the examples all have passing main methods
 */
object ExampleTests extends TestSuite {
  def tests = Tests {
    test("postgres") - example.PostgresExample.main(Array())
    test("mysql") - example.MySqlExample.main(Array())
    test("h2") - example.H2Example.main(Array())
    test("hsqldb") - example.HsqlDbExample.main(Array())
    test("sqlite") - example.SqliteExample.main(Array())
    test("hikari") - example.HikariCpExample.main(Array())
  }
}
