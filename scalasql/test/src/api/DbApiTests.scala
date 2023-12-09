package scalasql.api

import geny.Generator
import scalasql.core.SqlStr.SqlStringSyntax
import scalasql.{Buyer, Sc}
import scalasql.utils.{MySqlSuite, ScalaSqlSuite}
import sourcecode.Text
import utest._

import java.time.LocalDate
import scala.collection.mutable

trait DbApiTests extends ScalaSqlSuite {
  def description = "Basic usage of `db.*` operations such as `db.run`"
  override def utestBeforeEach(path: Seq[String]): Unit = checker.reset()

  def tests = Tests {
    test("renderSql") - checker.recorded(
      """
      You can use `.renderSql` on the `DbApi` or `DbClient` to see the SQL
      that is generated without actually running it
      """,
      Text {
        dbClient.renderSql(Buyer.select) ==>
          "SELECT buyer0.id AS id, buyer0.name AS name, buyer0.date_of_birth AS date_of_birth FROM buyer buyer0"
      }
    )
    test("run") - checker.recorded(
      """
      Most common usage of `dbClient.transaction`/`db.run`
      to run a simple query within a transaction
      """,
      Text {
        dbClient.transaction { db =>
          db.run(Buyer.select) ==> List(
            Buyer[Sc](1, "James Bond", LocalDate.parse("2001-02-03")),
            Buyer[Sc](2, "叉烧包", LocalDate.parse("1923-11-12")),
            Buyer[Sc](3, "Li Haoyi", LocalDate.parse("1965-08-09"))
          )
        }
      }
    )

    test("runSql") - {
      if (!this.isInstanceOf[MySqlSuite]) {
        checker.recorded(
          """
          `db.runSql` can be used to run `sql"..."` strings, while providing a
          specified type that the query results will be deserialized as the specified
          type. `db.runSql` supports the all the same data types as `db.run`:
          primitives, date and time types, tuples, `Foo[Sc]` `case class`s, and
          any combination of these.

          The `sql"..."` string interpolator automatically converts interpolated values
          into prepared statement variables, avoidin SQL injection vulnerabilities. You
          can also interpolate other `sql"..."` strings, or finally use `SqlStr.raw` for
          the rare cases where you want to interpolate a trusted `java.lang.String` into
          the `sql"..."` query without escaping.
          """,
          Text {

            dbClient.transaction { db =>
              val filterId = 2
              val output = db.runSql[String](
                sql"SELECT name FROM buyer WHERE id = $filterId"
              )

              assert(output == Seq("叉烧包"))

              val output2 = db.runSql[(String, LocalDate)](
                sql"SELECT name, date_of_birth FROM buyer WHERE id = $filterId"
              )
              assert(
                output2 ==
                  Seq(("叉烧包", LocalDate.parse("1923-11-12")))
              )

              val output3 = db.runSql[(String, LocalDate, Buyer[Sc])](
                sql"SELECT name, date_of_birth, * FROM buyer WHERE id = $filterId"
              )
              assert(
                output3 ==
                  Seq(
                    (
                      "叉烧包",
                      LocalDate.parse("1923-11-12"),
                      Buyer[Sc](
                        id = 2,
                        name = "叉烧包",
                        dateOfBirth = LocalDate.parse("1923-11-12")
                      )
                    )
                  )
              )
            }
          }
        )
      }
    }

    test("updateSql") - checker.recorded(
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
            .updateSql(
              sql"INSERT INTO buyer (name, date_of_birth) VALUES($newName, $newDateOfBirth)"
            )
          assert(count == 1)

          db.run(Buyer.select) ==> List(
            Buyer[Sc](1, "James Bond", LocalDate.parse("2001-02-03")),
            Buyer[Sc](2, "叉烧包", LocalDate.parse("1923-11-12")),
            Buyer[Sc](3, "Li Haoyi", LocalDate.parse("1965-08-09")),
            Buyer[Sc](4, "Moo Moo Cow", LocalDate.parse("2000-01-01"))
          )
        }
      }
    )

    test("runRaw") - checker.recorded(
      """
      `runRawQuery` is similar to `runQuery` but allows you to pass in the SQL strings
      "raw", along with `?` placeholders and interpolated variables passed separately.
      """,
      Text {
        dbClient.transaction { db =>
          val output = db.runRaw[String]("SELECT name FROM buyer WHERE id = ?", Seq(2))
          assert(output == Seq("叉烧包"))
        }
      }
    )

    test("updateRaw") - checker.recorded(
      """
      `runRawUpdate` is similar to `runRawQuery`, but for update queries that
      return a single number
      """,
      Text {
        dbClient.transaction { db =>
          val count = db.updateRaw(
            "INSERT INTO buyer (name, date_of_birth) VALUES(?, ?)",
            Seq("Moo Moo Cow", LocalDate.parse("2000-01-01"))
          )
          assert(count == 1)

          db.run(Buyer.select) ==> List(
            Buyer[Sc](1, "James Bond", LocalDate.parse("2001-02-03")),
            Buyer[Sc](2, "叉烧包", LocalDate.parse("1923-11-12")),
            Buyer[Sc](3, "Li Haoyi", LocalDate.parse("1965-08-09")),
            Buyer[Sc](4, "Moo Moo Cow", LocalDate.parse("2000-01-01"))
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

    test("streamSql") - checker.recorded(
      """
      `.streamSql` provides a lower level interface to `.stream`, allowing you to pass
      in a `SqlStr` of the form `sql"..."`, while allowing you to process the returned rows
      in a streaming fashion without.
      """,
      Text {
        dbClient.transaction { db =>
          val excluded = "James Bond"
          val output = db
            .streamSql[Buyer[Sc]](sql"SELECT * FROM buyer where name != $excluded")
            .takeWhile(_.id <= 2)
            .map(_.name)
            .toList

          output ==> List("叉烧包")
        }
      }
    )

    test("streamRaw") - checker.recorded(
      """
      `.streamRaw` provides a lowest level interface to `.stream`, allowing you to pass
      in a `java.lang.String` and a `Seq[Any]` representing the interpolated prepared
      statement variables
      """,
      Text {
        dbClient.transaction { db =>
          val excluded = "James Bond"
          val output = db
            .streamRaw[Buyer[Sc]]("SELECT * FROM buyer WHERE buyer.name <> ?", Seq(excluded))
            .takeWhile(_.id <= 2)
            .map(_.name)
            .toList

          output ==> List("叉烧包")
        }
      }
    )
  }
}
