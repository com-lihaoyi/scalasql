package usql

import dialects.{Dialect, H2Dialect, HsqlDbDialect, MySqlDialect, PostgresDialect, SqliteDialect}
import usql.renderer.SqlStr.SqlStringSyntax
import utest.TestSuite

import java.sql.DriverManager

trait UsqlTestSuite extends TestSuite with Dialect {
  val checker: TestDb
}
trait SqliteSuite extends TestSuite with SqliteDialect {
  val checker = new TestDb(
    DriverManager.getConnection("jdbc:sqlite::memory:"),
    "sqlite-test-data.sql",
    "",
    castParams = false
  )

  checker.reset()
}

trait HsqlDbSuite extends TestSuite with HsqlDbDialect {
  val checker = new TestDb(
    DriverManager.getConnection("jdbc:hsqldb:mem:mydb"),
    "hsqldb-test-data.sql",
    " FROM (VALUES (0))",
    castParams = true
  )

  checker.reset()
}

trait H2Suite extends TestSuite with H2Dialect {
  val checker = new TestDb(
    DriverManager.getConnection("jdbc:h2:mem:mydb"),
    "h2-test-data.sql",
    "",
    castParams = true
  )

  checker.reset()
}

trait PostgresSuite extends TestSuite with PostgresDialect {
  val checker = new TestDb(
    DriverManager.getConnection(
      s"${TestDb.pg.getJdbcUrl}&user=${TestDb.pg.getUsername}&password=${TestDb.pg.getPassword}"
    ),
    "postgres-test-data.sql",
    "",
    castParams = false
  )

  checker.reset()
}

trait MySqlSuite extends TestSuite with MySqlDialect {
  val checker = new TestDb(
    DriverManager.getConnection(
      TestDb.mysql.getJdbcUrl + "?allowMultiQueries=true",
      TestDb.mysql.getUsername,
      TestDb.mysql.getPassword
    ),
    "mysql-test-data.sql",
    "",
    castParams = false
  )

  checker.reset()
}