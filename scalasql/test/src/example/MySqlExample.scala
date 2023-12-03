package scalasql.example

import java.sql.DriverManager
import org.testcontainers.containers.MySQLContainer
import scalasql.Table
import scalasql.MySqlDialect._
object MySqlExample {

  case class ExampleProduct[T[_]](
      id: T[Int],
      kebabCaseName: T[String],
      name: T[String],
      price: T[Double]
  )

  object ExampleProduct extends Table[ExampleProduct]

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
    dialect = scalasql.PostgresDialect,
    config = new scalasql.Config {}
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
    }
  }
}
