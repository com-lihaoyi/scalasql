// duplicated from scalasql/test/src/example/MySqlExample.scala
package scalasql.simple.example

import org.testcontainers.containers.MySQLContainer

import scalasql.simple.{*, given}
import MySqlDialect.*

object SimpleTableMySqlExample {

  case class ExampleProduct(
      id: Int,
      kebabCaseName: String,
      name: String,
      price: Double
  )

  object ExampleProduct extends SimpleTable[ExampleProduct]

  // The example MySQLContainer comes from the library `org.testcontainers:mysql:1.19.1`
  lazy val mysql = {
    println("Initializing MySql")
    val mysql = new MySQLContainer("mysql:8.0.31")
    mysql.setCommand("mysqld", "--character-set-server=utf8mb4", "--collation-server=utf8mb4_bin")
    mysql.start()
    mysql
  }

  val dataSource = new com.mysql.cj.jdbc.MysqlDataSource
  dataSource.setURL(mysql.getJdbcUrl + "?allowMultiQueries=true")
  dataSource.setDatabaseName(mysql.getDatabaseName);
  dataSource.setUser(mysql.getUsername);
  dataSource.setPassword(mysql.getPassword);

  lazy val mysqlClient = new DbClient.DataSource(
    dataSource,
    config = new {}
  )

  def main(args: Array[String]): Unit = {
    mysqlClient.transaction { db =>
      db.updateRaw("""
      CREATE TABLE example_product (
          id INTEGER PRIMARY KEY AUTO_INCREMENT,
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

      val result3 =
        db.run(
          ExampleProduct.select
            .filter(_.price > 10)
            .sortBy(_.price)
            .desc
            .map(p => (name = p.name, price = p.price))
        )

      assert(
        result3 == Seq(
          (name = "Camera", price = 1000.00),
          (name = "Skate Board", price = 123.45),
          (name = "Cookie", price = 11.0)
        )
      )
    }
  }
}
