// duplicated from scalasql/test/src/example/PostgresExample.scala
package scalasql.namedtuples.example

import org.testcontainers.containers.PostgreSQLContainer

import scalasql.simple.{*, given}
import PostgresDialect.*

object SimpleTablePostgresExample {

  case class ExampleProduct(
      id: Int,
      kebabCaseName: String,
      name: String,
      price: Double
  )

  object ExampleProduct extends SimpleTable[ExampleProduct]

  // The example PostgreSQLContainer comes from the library `org.testcontainers:postgresql:1.19.1`
  lazy val postgres = {
    println("Initializing Postgres")
    val pg = new PostgreSQLContainer("postgres:15-alpine")
    pg.start()
    pg
  }

  val dataSource = new org.postgresql.ds.PGSimpleDataSource
  dataSource.setURL(postgres.getJdbcUrl)
  dataSource.setDatabaseName(postgres.getDatabaseName);
  dataSource.setUser(postgres.getUsername);
  dataSource.setPassword(postgres.getPassword);

  lazy val postgresClient = new DbClient.DataSource(
    dataSource,
    config = new {}
  )

  def main(args: Array[String]): Unit = {
    postgresClient.transaction { db =>
      db.updateRaw("""
      CREATE TABLE example_product (
          id SERIAL PRIMARY KEY,
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
