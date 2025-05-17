// duplicated from scalasql/test/src/example/H2Example.scala
package scalasql.namedtuples.example

import scalasql.namedtuples.SimpleTable
import scalasql.namedtuples.SimpleTable.NamedTupleOps.given
import scalasql.H2Dialect.*

object SimpleTableH2Example {

  case class ExampleProduct(
      id: Int,
      kebabCaseName: String,
      name: String,
      price: Double
  )

  object ExampleProduct extends SimpleTable[ExampleProduct]

  // The example H2 database comes from the library `com.h2database:h2:2.2.224`
  val dataSource = new org.h2.jdbcx.JdbcDataSource
  dataSource.setUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1")
  lazy val h2Client = new scalasql.DbClient.DataSource(
    dataSource,
    config = new scalasql.Config {}
  )

  def main(args: Array[String]): Unit = {
    h2Client.transaction { db =>
      db.updateRaw("""
      CREATE TABLE example_product (
          id INTEGER AUTO_INCREMENT PRIMARY KEY,
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
