// duplicated from scalasql/test/src/example/SqliteExample.scala
package scalasql.namedtuples.example

import scalasql.namedtuples.SimpleTable
import scalasql.namedtuples.NamedTupleQueryable.given

import scalasql.SqliteDialect.*
object SimpleTableSqliteExample {

  case class ExampleProduct(
      id: Int,
      kebabCaseName: String,
      name: String,
      price: Double
  )

  object ExampleProduct extends SimpleTable[ExampleProduct]

  // The example Sqlite JDBC client comes from the library `org.xerial:sqlite-jdbc:3.43.0.0`
  val dataSource = new org.sqlite.SQLiteDataSource()
  val tmpDb = java.nio.file.Files.createTempDirectory("sqlite")
  dataSource.setUrl(s"jdbc:sqlite:$tmpDb/file.db")
  lazy val sqliteClient = new scalasql.DbClient.DataSource(
    dataSource,
    config = new scalasql.Config {}
  )

  def main(args: Array[String]): Unit = {
    sqliteClient.transaction { db =>
      db.updateRaw("""
      CREATE TABLE example_product (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
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
