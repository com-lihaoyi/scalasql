package scalasql.utils

import scalasql.dialects
import scalasql.dialects._
import utest.TestSuite

import java.sql.DriverManager

abstract class ScalaSqlSuite(implicit val suiteLine: sourcecode.Line)
    extends TestSuite
    with Dialect {
  def checker: TestDb

  lazy val dbClient = checker.dbClient
  def description: String
}

trait SqliteSuite extends ScalaSqlSuite with SqliteDialect {
  val checker = new TestDb(
    TestClients.sqliteClient,
    "sqlite-customer-schema.sql",
    "customer-data.sql",
    dialects.SqliteDialect,
    getClass.getName,
    suiteLine.value,
    description
  )

  checker.reset()
}

trait HsqlDbSuite extends ScalaSqlSuite with HsqlDbDialect {
  val checker = new TestDb(
    TestClients.hsqlDbClient,
    "hsqldb-customer-schema.sql",
    "customer-data.sql",
    dialects.HsqlDbDialect,
    getClass.getName,
    suiteLine.value,
    description
  )

  checker.reset()
}

trait H2Suite extends ScalaSqlSuite with H2Dialect {
  val checker = new TestDb(
    TestClients.h2Client,
    "h2-customer-schema.sql",
    "customer-data.sql",
    dialects.H2Dialect,
    getClass.getName,
    suiteLine.value,
    description
  )

  checker.reset()
}

trait PostgresSuite extends ScalaSqlSuite with PostgresDialect {
  val checker = new TestDb(
    TestClients.postgresClient,
    "postgres-customer-schema.sql",
    "customer-data.sql",
    dialects.PostgresDialect,
    getClass.getName,
    suiteLine.value,
    description
  )

  checker.reset()
}

trait MySqlSuite extends ScalaSqlSuite with MySqlDialect {
  val checker = new TestDb(
    TestClients.mysqlClient,
    "mysql-customer-schema.sql",
    "customer-data.sql",
    dialects.MySqlDialect,
    getClass.getName,
    suiteLine.value,
    description
  )

  checker.reset()
}
