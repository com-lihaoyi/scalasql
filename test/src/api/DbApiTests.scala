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
    test("run") - checker.recorded(Text {
      dbClient.transaction { db =>
        db.run(Buyer.select) ==> List(
          Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")),
          Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
          Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09"))
        )
      }
    })

    test("runQuery") - checker.recorded(Text {
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
    })

    test("runUpdate") - checker.recorded(Text {
      dbClient.transaction { db =>
        val newName = "Moo Moo Cow"
        val newDateOfBirth = LocalDate.parse("2000-01-01")
        val count = db
          .runUpdate(sql"INSERT INTO buyer (name, date_of_birth) VALUES($newName, $newDateOfBirth)")
        assert(count == 1)

        db.run(Buyer.select) ==> List(
          Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")),
          Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
          Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09")),
          Buyer[Id](4, "Moo Moo Cow", LocalDate.parse("2000-01-01"))
        )
      }
    })

    test("runRawQuery") {
      test("simple") - checker.recorded(Text {
        dbClient.transaction { db =>
          val output = db.runRawQuery("SELECT name FROM buyer") { rs =>
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

          assert(output == Seq("James Bond", "叉烧包", "Li Haoyi"))
        }
      })

      test("interpolated") - checker.recorded(Text {
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
      })

    }

    test("runRawUpdate") {
      test("Simple") - checker.recorded(Text {
        dbClient.transaction { db =>
          val count = db.runRawUpdate(
            "INSERT INTO buyer (name, date_of_birth) VALUES('Moo Moo Cow', '2000-01-01')"
          )
          assert(count == 1)

          db.run(Buyer.select) ==> List(
            Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")),
            Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
            Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09")),
            Buyer[Id](4, "Moo Moo Cow", LocalDate.parse("2000-01-01"))
          )
        }
      })

      test("prepared") - checker.recorded(Text {
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
      })
    }

    test("stream") - checker.recorded(Text {
      dbClient.transaction { db =>
        val output = collection.mutable.Buffer.empty[String]

        db.stream(Buyer.select).generate { buyer =>
          output.append(buyer.name)
          if (buyer.id >= 2) Generator.End else Generator.Continue
        }

        output ==> List("James Bond", "叉烧包")
      }
    })
  }
}
