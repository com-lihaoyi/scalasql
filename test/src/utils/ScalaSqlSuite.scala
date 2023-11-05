package scalasql.utils

import scalasql.dialects
import scalasql.dialects._
import utest.TestSuite

import java.sql.DriverManager

trait ScalaSqlSuite extends TestSuite with Dialect {
  def checker: TestDb
  lazy val dbClient = checker.dbClient
}

trait SqliteSuite extends TestSuite with SqliteDialect {
  val checker = new TestDb(
    TestClients.sqliteClient,
    "sqlite-customer-schema.sql",
    "customer-data.sql",
    dialects.SqliteDialect,
    getClass.getName
  )

  checker.reset()
}

trait HsqlDbSuite extends TestSuite with HsqlDbDialect {
  val checker = new TestDb(
    TestClients.hsqlDbClient,
    "hsqldb-customer-schema.sql",
    "customer-data.sql",
    dialects.HsqlDbDialect,
    getClass.getName
  )

  checker.reset()
}

trait H2Suite extends TestSuite with H2Dialect {
  val checker = new TestDb(
    TestClients.h2Client,
    "h2-customer-schema.sql",
    "customer-data.sql",
    dialects.H2Dialect,
    getClass.getName
  )

  checker.reset()
}

trait PostgresSuite extends TestSuite with PostgresDialect {
  val checker = new TestDb(
    TestClients.postgresClient,
    "postgres-customer-schema.sql",
    "customer-data.sql",
    dialects.PostgresDialect,
    getClass.getName
  )

  checker.reset()
}

trait MySqlSuite extends TestSuite with MySqlDialect {
  val checker = new TestDb(
    TestClients.mysqlClient,
    "mysql-customer-schema.sql",
    "customer-data.sql",
    dialects.MySqlDialect,
    getClass.getName
  )

  checker.reset()
}
