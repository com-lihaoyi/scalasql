package scalasql.utils

import scalasql.{DatabaseClient, DbApi, dialects}
import scalasql.dialects._
import utest.TestSuite

import java.sql.DriverManager

abstract class ScalaSqlSuite(implicit val suiteLine: sourcecode.Line)
    extends TestSuite
    with Dialect {
  def checker: TestChecker

  lazy val dbClient = checker.dbClient
  def description: String
}

trait SqliteSuite extends ScalaSqlSuite with SqliteDialect {
  val checker = new TestChecker(
    scalasql.example.SqliteExample.sqliteClient,
    "sqlite-customer-schema.sql",
    "customer-data.sql",
    getClass.getName,
    suiteLine.value,
    description
  )

  checker.reset()
}

trait H2Suite extends ScalaSqlSuite with H2Dialect {
  val checker = new TestChecker(
    scalasql.example.H2Example.h2Client,
    "h2-customer-schema.sql",
    "customer-data.sql",
    getClass.getName,
    suiteLine.value,
    description
  )

  checker.reset()
}

trait PostgresSuite extends ScalaSqlSuite with PostgresDialect {
  val checker = new TestChecker(
    scalasql.example.PostgresExample.postgresClient,
    "postgres-customer-schema.sql",
    "customer-data.sql",
    getClass.getName,
    suiteLine.value,
    description
  )

  checker.reset()
}

trait HikariSuite extends ScalaSqlSuite with PostgresDialect {
  val checker = new TestChecker(
    scalasql.example.HikariCpExample.hikariClient,
    "postgres-customer-schema.sql",
    "customer-data.sql",
    getClass.getName,
    suiteLine.value,
    description
  )

  checker.reset()

  override def utestAfterAll(): Unit = {
    super.utestAfterAll()
    checker.autoCommitConnection.close()
  }
}

trait MySqlSuite extends ScalaSqlSuite with MySqlDialect {
  val checker = new TestChecker(
    scalasql.example.MySqlExample.mysqlClient,
    "mysql-customer-schema.sql",
    "customer-data.sql",
    getClass.getName,
    suiteLine.value,
    description
  )

  checker.reset()
}
