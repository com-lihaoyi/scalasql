package scalasql.api

import geny.Generator
import scalasql.{Buyer, Id}
import scalasql.utils.ScalaSqlSuite
import utest._

import java.time.LocalDate

trait DbApiTests extends ScalaSqlSuite {
  class FooException extends Exception

  def tests = Tests {
    test("run") {
      checker.db.transaction{db =>
        db.run(Buyer.select) ==> List(
          Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")),
          Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
          Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09"))
        )
      }
    }
    test("stream") {
      checker.db.transaction{db =>
        val output = collection.mutable.Buffer.empty[String]

        db.stream(Buyer.select).generate{buyer =>
          output.append(buyer.name)
          if (buyer.id >= 2) Generator.End
          else Generator.Continue
        }

        output ==> List("James Bond", "叉烧包")
      }
    }
  }
}
