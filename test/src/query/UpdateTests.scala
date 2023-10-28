package scalasql.query

import scalasql._
import utest._

import java.time.LocalDate

/**
 * Tests for basic update operations
 */
trait UpdateTests extends ScalaSqlSuite {
  override def utestBeforeEach(path: Seq[String]): Unit = checker.reset()
  def tests = Tests {
    test("update") - {
      checker(
        query = Buyer.update.filter(_.name === "James Bond")
          .set(_.dateOfBirth -> LocalDate.parse("2019-04-07")),
        sqls = Seq(
          "UPDATE buyer SET date_of_birth = ? WHERE buyer.name = ?",
          "UPDATE buyer SET buyer.date_of_birth = ? WHERE buyer.name = ?"
        ),
        value = 1
      )

      checker(
        query = Buyer.select.filter(_.name === "James Bond").map(_.dateOfBirth),
        value = Seq(LocalDate.parse("2019-04-07"))
      )

      checker(
        query = Buyer.select.filter(_.name === "Li Haoyi").map(_.dateOfBirth),
        value = Seq(LocalDate.parse("1965-08-09")) // not updated
      )
    }

    test("bulk") - {
      checker(
        query = Buyer.update.set(_.dateOfBirth -> LocalDate.parse("2019-04-07")),
        sqls =
          Seq("UPDATE buyer SET date_of_birth = ?", "UPDATE buyer SET buyer.date_of_birth = ?"),
        value = 3
      )

      checker(
        query = Buyer.select.filter(_.name === "James Bond").map(_.dateOfBirth),
        value = Seq(LocalDate.parse("2019-04-07"))
      )
      checker(
        query = Buyer.select.filter(_.name === "Li Haoyi").map(_.dateOfBirth),
        value = Seq(LocalDate.parse("2019-04-07"))
      )
    }

    test("multiple") - {
      checker(
        query = Buyer.update.filter(_.name === "James Bond")
          .set(_.dateOfBirth -> LocalDate.parse("2019-04-07"), _.name -> "John Dee"),
        sqls = Seq(
          "UPDATE buyer SET date_of_birth = ?, name = ? WHERE buyer.name = ?",
          "UPDATE buyer SET buyer.date_of_birth = ?, buyer.name = ? WHERE buyer.name = ?"
        ),
        value = 1
      )

      checker(
        query = Buyer.select.filter(_.name === "James Bond").map(_.dateOfBirth),
        value = Seq[LocalDate]()
      )

      checker(
        query = Buyer.select.filter(_.name === "John Dee").map(_.dateOfBirth),
        value = Seq(LocalDate.parse("2019-04-07"))
      )
    }

    test("dynamic") - {
      checker(
        query = Buyer.update.filter(_.name === "James Bond").set(c => c.name -> c.name.toUpperCase),
        sqls = Seq(
          "UPDATE buyer SET name = UPPER(buyer.name) WHERE buyer.name = ?",
          "UPDATE buyer SET buyer.name = UPPER(buyer.name) WHERE buyer.name = ?"
        ),
        value = 1
      )

      checker(
        query = Buyer.select.filter(_.name === "James Bond").map(_.dateOfBirth),
        value = Seq[LocalDate]()
      )

      checker(
        query = Buyer.select.filter(_.name === "JAMES BOND").map(_.dateOfBirth),
        value = Seq(LocalDate.parse("2001-02-03"))
      )
    }

  }
}
