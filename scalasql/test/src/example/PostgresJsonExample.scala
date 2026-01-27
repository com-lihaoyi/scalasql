package scalasql.example

import org.testcontainers.containers.PostgreSQLContainer
import scalasql.Table
import scalasql.PostgresDialect._
import scalasql.core.Expr
import ujson.Value

object PostgresJsonExample {

  case class Person[T[_]](
      id: T[Int],
      name: T[String],
      info: T[ujson.Value]
  )

  object Person extends Table[Person]

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

  lazy val postgresClient = new scalasql.DbClient.DataSource(
    dataSource,
    config = new scalasql.Config {}
  )

  def main(args: Array[String]): Unit = {
    postgresClient.transaction { db =>
      db.updateRaw("""
      CREATE TABLE person (
          id SERIAL PRIMARY KEY,
          name VARCHAR(256),
          info JSONB
      );
      """)

      val inserted = db.run(
        Person.insert.batched(_.name, _.info)(
          ("John", ujson.Obj("age" -> 30, "pets" -> ujson.Arr("cat", "dog"), "active" -> true)),
          ("Jane", ujson.Obj("age" -> 25, "pets" -> ujson.Arr(), "active" -> false)),
          ("Bob", ujson.Obj("age" -> 40, "pets" -> ujson.Arr("fish"), "active" -> true))
        )
      )

      assert(inserted == 3)

      // Select with filter using JSON operator -> and casting
      // Find people older than 28
      // Note: -> returns JSON, so we cast to Int for comparison if we extracted as text,
      // but here we can rely on ujson comparison if we implement it,
      // or easier: extract as text and cast, or use @> for containment

      // Using ->> to get text and cast to integer
      val seniors = db.run(
        Person.select
          .filter(p => (p.info ->> "age").cast[Int] > 28)
          .map(_.name)
      )
      assert(seniors.toSet == Set("John", "Bob"))

      // Using @> (contains)
      // Find people who are active
      val active = db.run(
        Person.select
          .filter(p => p.info @> ujson.Obj("active" -> true))
          .map(_.name)
      )
      assert(active.toSet == Set("John", "Bob"))

      // Using ? (exists key)
      // Find people who have "pets" key (all of them)
      val hasPets = db.run(
        Person.select.filter(p => p.info ? "pets").size
      )
      assert(hasPets == 3)

      // Using -> and index access
      // Find people whose first pet is "cat"
      // p.info -> "pets" gives the array. -> 0 gives the first element.
      // We compare it to ujson.Str("cat")
      val catLovers = db.run(
        Person.select
          .filter(p => (p.info -> "pets" -> 0) === Expr[ujson.Value](ujson.Str("cat")))
          .map(_.name)
      )
      assert(catLovers == Seq("John"))

      // Update
      // Add a new field "city": "New York" to John
      db.run(
        Person
          .update(_.name === "John")
          .set(p => p.info := (p.info || ujson.Obj("city" -> "New York")))
      )

      val johnInfo = db.run(Person.select.filter(_.name === "John").single).info
      assert(johnInfo("city").str == "New York")
      assert(johnInfo("age").num == 30)

      // Delete key
      // Remove "active" field from Jane
      db.run(
        Person
          .update(_.name === "Jane")
          .set(p => p.info := (p.info - "active"))
      )

      val janeInfo = db.run(Person.select.filter(_.name === "Jane").single).info
      assert(!janeInfo.obj.contains("active"))
      assert(janeInfo("age").num == 25)
    }
  }
}
