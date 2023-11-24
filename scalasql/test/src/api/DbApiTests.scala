package scalasql.api

import geny.Generator
import scalasql.renderer.SqlStr.SqlStringSyntax
import scalasql.{Buyer, Id}
import scalasql.utils.ScalaSqlSuite
import sourcecode.Text
import utest._

import java.time.LocalDate
import scala.collection.mutable

trait DbApiTests extends ScalaSqlSuite {
  def description = "Basic usage of `db.*` operations such as `db.run`"
  override def utestBeforeEach(path: Seq[String]): Unit = checker.reset()

  def tests = Tests {
    test("run") - checker.recorded(
      """
      Most common usage of `dbClient.transaction`/`db.run`
      to run a simple query within a transaction
      """,
      Text {
        dbClient.transaction { db =>
          db.run(Buyer.select) ==> List(
            Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")),
            Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
            Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09"))
          )
        }
      }
    )

    test("runQuery0") - checker.recorded(
      """
      `db.runQuery` allows you to pass in a `SqlStr` using the `sql"..."` syntax,
      allowing you to construct SQL strings and interpolate variables within them.
      Interpolated variables automatically become prepared statement variables to
      avoid SQL injection vulnerabilities. Takes a callback providing a `java.sql.ResultSet`
      for you to use directly.
      """,
      Text {

        dbClient.transaction { db =>
          val filterId = 2
          val output = db.runQuery0[Seq[String]](sql"SELECT name FROM buyer WHERE id = $filterId")
          assert(output == Seq("叉烧包"))
        }
      }
    )

    test("runQuery") - checker.recorded(
      """
      `db.runQuery` allows you to pass in a `SqlStr` using the `sql"..."` syntax,
      allowing you to construct SQL strings and interpolate variables within them.
      Interpolated variables automatically become prepared statement variables to
      avoid SQL injection vulnerabilities. Takes a callback providing a `java.sql.ResultSet`
      for you to use directly.
      """,
      Text {

        dbClient.transaction { db =>
          val filterId = 2
          val output = db.runQuery(sql"SELECT name FROM buyer WHERE id = $filterId") { rs =>
            val output = mutable.Buffer.empty[String]

            while (
              rs.next() match {
                case false => false
                case true =>
                  output.append(rs.getString(1))
                  true
              }
            ) ()
            output
          }

          assert(output == Seq("叉烧包"))
        }
      }
    )

    test("runUpdate") - checker.recorded(
      """
      Similar to `db.runQuery`, `db.runUpdate` allows you to pass in a `SqlStr`, but runs
      an update rather than a query and expects to receive a single number back from the
      database indicating the number of rows inserted or updated
      """,
      Text {

        dbClient.transaction { db =>
          val newName = "Moo Moo Cow"
          val newDateOfBirth = LocalDate.parse("2000-01-01")
          val count = db
            .runUpdate(
              sql"INSERT INTO buyer (name, date_of_birth) VALUES($newName, $newDateOfBirth)"
            )
          assert(count == 1)

          db.run(Buyer.select) ==> List(
            Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")),
            Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
            Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09")),
            Buyer[Id](4, "Moo Moo Cow", LocalDate.parse("2000-01-01"))
          )
        }
      }
    )

    test("runRawQuery") - checker.recorded(
      """
      `runRawQuery` is similar to `runQuery` but allows you to pass in the SQL strings
      "raw", along with `?` placeholders and interpolated variables passed separately.
      """,
      Text {
        dbClient.transaction { db =>
          val output = db.runRawQuery("SELECT name FROM buyer WHERE id = ?", 2) { rs =>
            val output = mutable.Buffer.empty[String]

            while (
              rs.next() match {
                case false => false
                case true =>
                  output.append(rs.getString(1))
                  true
              }
            ) ()
            output
          }

          assert(output == Seq("叉烧包"))
        }
      }
    )

    test("runRawUpdate") - checker.recorded(
      """
      `runRawUpdate` is similar to `runRawQuery`, but for update queries that
      return a single number
      """,
      Text {
        dbClient.transaction { db =>
          val count = db.runRawUpdate(
            "INSERT INTO buyer (name, date_of_birth) VALUES(?, ?)",
            "Moo Moo Cow",
            LocalDate.parse("2000-01-01")
          )
          assert(count == 1)

          db.run(Buyer.select) ==> List(
            Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")),
            Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
            Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09")),
            Buyer[Id](4, "Moo Moo Cow", LocalDate.parse("2000-01-01"))
          )
        }
      }
    )

    test("stream") - checker.recorded(
      """
      `db.stream` can be run on queries that return `Seq[T]`s, and makes them
      return `geny.Generator[T]`s instead. This allows you to deserialize and
      process the returned database rows incrementally without buffering the
      entire `Seq[T]` in memory. Not that the underlying JDBC driver and the
      underlying database may each perform their own buffering depending on
      their implementation
      """,
      Text {
        dbClient.transaction { db =>
          val output = collection.mutable.Buffer.empty[String]

          db.stream(Buyer.select).generate { buyer =>
            output.append(buyer.name)
            if (buyer.id >= 2) Generator.End else Generator.Continue
          }

          output ==> List("James Bond", "叉烧包")
        }
      }
    )
  }
}
