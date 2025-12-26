package scalasql.simple

import utest._

object SimpleTableExampleTests extends TestSuite:
  def tests = Tests:
    test("postgres") - example.SimpleTablePostgresExample.main(Array.empty)
    test("mysql") - example.SimpleTableMySqlExample.main(Array.empty)
    test("h2") - example.SimpleTableH2Example.main(Array.empty)
    test("sqlite") - example.SimpleTableSqliteExample.main(Array.empty)
