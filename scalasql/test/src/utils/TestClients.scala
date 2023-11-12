package scalasql.utils


object TestClients {

  // +DOCS
  // ## Connecting to Your Database
  //
  // This section contains examples of how to connect to the different databases
  // that ScalaSql supports: Sqlite, HsqlDb, H2, MySql, Postgres, as well as an
  // example of using a connection pool like HikariCP. These are all connections
  // to databases running in-memory or in local docker containers suitable for
  // testing, but you can replace the url/username/password to connect to production
  // databases running on datacenter or cloud infrastructure
  import org.testcontainers.containers.{MySQLContainer, PostgreSQLContainer}
  import java.sql.DriverManager


  // ### Sqlite
  // The example Sqlite JDBC client comes from the library `org.xerial:sqlite-jdbc:3.43.0.0`
  lazy val sqliteClient = new scalasql.DatabaseClient.Connection(
    DriverManager.getConnection("jdbc:sqlite::memory:"),
    dialectConfig = scalasql.dialects.SqliteDialect,
    config = new scalasql.Config {}
  )

  // ### HsqlDB
  // The example HsqlDB database comes from the library `org.hsqldb:hsqldb:2.5.1`
  lazy val hsqlDbClient = new scalasql.DatabaseClient.Connection(
    DriverManager.getConnection("jdbc:hsqldb:mem:mydb"),
    dialectConfig = scalasql.dialects.HsqlDbDialect,
    config = new scalasql.Config {}
  )

  // ### H2
  // The example H2 database comes from the library `com.h2database:h2:2.2.224`
  lazy val h2Client = new scalasql.DatabaseClient.Connection(
    DriverManager.getConnection("jdbc:h2:mem:mydb"),
    dialectConfig = scalasql.dialects.H2Dialect,
    config = new scalasql.Config {}
  )

  // ### MySql
  // The example MySQLContainer comes from the library `org.testcontainers:mysql:1.19.1`
  lazy val mysql = {
    println("Initializing MySql")
    val mysql = new MySQLContainer("mysql:8.0.31")
    mysql.setCommand("mysqld", "--character-set-server=utf8mb4", "--collation-server=utf8mb4_bin")
    mysql.start()
    mysql
  }

  lazy val mysqlClient = new scalasql.DatabaseClient.Connection(
    DriverManager.getConnection(
      mysql.getJdbcUrl + "?allowMultiQueries=true",
      mysql.getUsername,
      mysql.getPassword
    ),
    dialectConfig = scalasql.dialects.PostgresDialect,
    config = new scalasql.Config {}
  )

  // ### Postgres
  // The example PostgreSQLContainer comes from the library `org.testcontainers:postgresql:1.19.1`
  lazy val postgres = {
    println("Initializing Postgres")
    val pg = new PostgreSQLContainer("postgres:15-alpine")
    pg.start()
    pg
  }

  lazy val postgresClient = new scalasql.DatabaseClient.Connection(
    DriverManager.getConnection(postgres.getJdbcUrl, postgres.getUsername, postgres.getPassword),
    dialectConfig = scalasql.dialects.PostgresDialect,
    config = new scalasql.Config {}
  )

  // ### HikariCP
  // HikariDataSource comes from the library `com.zaxxer:HikariCP:5.1.0`
  val hikariDataSource = new com.zaxxer.hikari.HikariDataSource()
  hikariDataSource.setJdbcUrl(postgres.getJdbcUrl)
  hikariDataSource.setUsername(postgres.getUsername)
  hikariDataSource.setPassword(postgres.getPassword)

  lazy val postgresHiikariClient = new scalasql.DatabaseClient.DataSource(
    hikariDataSource,
    dialectConfig = scalasql.dialects.PostgresDialect,
    config = new scalasql.Config {}
  )
  // -DOCS
}
