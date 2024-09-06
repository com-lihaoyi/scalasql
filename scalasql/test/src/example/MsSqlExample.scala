package scalasql.example

import org.testcontainers.containers.MSSQLServerContainer
import scalasql.Table
import scalasql.MsSqlDialect._

object MsSqlExample {
  case class ExampleProduct[T[_]](
      id: T[Int],
      kebabCaseName: T[String],
      name: T[String],
      price: T[Double]
  )

  object ExampleProduct extends Table[ExampleProduct]

  lazy val mssql = {
    println("Initializing MsSql")
    val mssql = new MSSQLServerContainer("mcr.microsoft.com/mssql/server:2022-CU14-ubuntu-22.04")
    mssql.acceptLicense()
    mssql.addEnv("MSSQL_COLLATION", "Latin1_General_100_CI_AS_SC_UTF8")
    mssql.start()
    mssql
  }

  val dataSource = new com.microsoft.sqlserver.jdbc.SQLServerDataSource
  dataSource.setURL(mssql.getJdbcUrl)
  dataSource.setUser(mssql.getUsername)
  dataSource.setPassword(mssql.getPassword)

  lazy val mssqlClient = new scalasql.DbClient.DataSource(
    dataSource,
    config = new scalasql.Config {}
  )

  def main(args: Array[String]): Unit = {
    mssqlClient.transaction { db =>
      db.updateRaw("""
      CREATE TABLE example_product (
          id INT PRIMARY KEY IDENTITY(1, 1),
          kebab_case_name VARCHAR(256),
          name VARCHAR(256),
          price DECIMAL(20, 2)
      );
      """)

      val inserted = db.run(
        ExampleProduct.insert.batched(_.kebabCaseName, _.name, _.price)(
          ("face-mask", "Face Mask", 8.88),
          ("guitar", "Guitar", 300),
          ("socks", "Socks", 3.14),
          ("skate-board", "Skate Board", 123.45),
          ("camera", "Camera", 1000.00),
          ("cookie", "Cookie", 0.10)
        )
      )

      assert(inserted == 6)

      val result =
        db.run(ExampleProduct.select.filter(_.price > 10).sortBy(_.price).desc.map(_.name))

      assert(result == Seq("Camera", "Guitar", "Skate Board"))

      db.run(ExampleProduct.update(_.name === "Cookie").set(_.price := 11.0))

      db.run(ExampleProduct.delete(_.name === "Guitar"))

      val result2 =
        db.run(ExampleProduct.select.filter(_.price > 10).sortBy(_.price).desc.map(_.name))

      assert(result2 == Seq("Camera", "Skate Board", "Cookie"))
    }
  }
}
