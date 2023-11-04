package scalasql.utils

import org.testcontainers.containers.{MySQLContainer, PostgreSQLContainer}

object TestClients{
  import java.sql.DriverManager

  lazy val sqliteClient = new scalasql.DatabaseClient(
    DriverManager.getConnection("jdbc:sqlite::memory:"),
    dialectConfig = scalasql.dialects.SqliteDialect,
    config = new scalasql.Config{}
  )

  lazy val hsqlDbClient = new scalasql.DatabaseClient(
    DriverManager.getConnection("jdbc:hsqldb:mem:mydb"),
    dialectConfig = scalasql.dialects.HsqlDbDialect,
    config = new scalasql.Config{}
  )

  lazy val h2Client = new scalasql.DatabaseClient(
    DriverManager.getConnection("jdbc:h2:mem:mydb"),
    dialectConfig = scalasql.dialects.H2Dialect,
    config = new scalasql.Config{}
  )

  lazy val postgres = {
    println("Initializing Postgres")
    val pg = new PostgreSQLContainer("postgres:15-alpine")
    pg.start()
    pg
  }

  lazy val postgresClient = new scalasql.DatabaseClient(
    DriverManager.getConnection(postgres.getJdbcUrl, postgres.getUsername, postgres.getPassword),
    dialectConfig = scalasql.dialects.PostgresDialect,
    config = new scalasql.Config{}
  )

  lazy val mysql = {
    println("Initializing MySql")
    val mysql = new MySQLContainer("mysql:8.0.31")
    mysql.setCommand("mysqld", "--character-set-server=utf8mb4", "--collation-server=utf8mb4_bin")
    mysql.start()
    mysql
  }

  lazy val mysqlClient = new scalasql.DatabaseClient(
    DriverManager.getConnection(
      mysql.getJdbcUrl + "?allowMultiQueries=true",
      mysql.getUsername,
      mysql.getPassword
    ),
    dialectConfig = scalasql.dialects.PostgresDialect,
    config = new scalasql.Config{}
  )

}