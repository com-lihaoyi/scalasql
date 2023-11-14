package scalasql.query

import scalasql._
import sourcecode.Text
import utest._
import utils.ScalaSqlSuite

import java.time.LocalDate

trait UpdateTests extends ScalaSqlSuite {
  def description = "Basic `UPDATE` queries"
  override def utestBeforeEach(path: Seq[String]): Unit = checker.reset()
  def tests = Tests {
    test("update") - {
      checker(
        query = Text {
          Buyer
            .update(_.name `=` "James Bond")
            .set(_.dateOfBirth := LocalDate.parse("2019-04-07"))
        },
        sqls = Seq(
          "UPDATE buyer SET date_of_birth = ? WHERE (buyer.name = ?)",
          "UPDATE buyer SET buyer.date_of_birth = ? WHERE (buyer.name = ?)"
        ),
        value = 1,
        docs = """
          `Table.update` takes a predicate specifying the rows to update, and a
          `.set` clause that allows you to specify the values assigned to columns
          on those rows
        """
      )

      checker(
        query = Text {
          Buyer.select.filter(_.name `=` "James Bond").map(_.dateOfBirth).single
        },
        value = LocalDate.parse("2019-04-07")
      )

      checker(
        query = Text {
          Buyer.select.filter(_.name `=` "Li Haoyi").map(_.dateOfBirth).single
        },
        value = LocalDate.parse("1965-08-09" /* not updated */ )
      )
    }

    test("bulk") - {
      checker(
        query = Text {
          Buyer.update(_ => true).set(_.dateOfBirth := LocalDate.parse("2019-04-07"))
        },
        sqls = Seq(
          "UPDATE buyer SET date_of_birth = ?",
          "UPDATE buyer SET buyer.date_of_birth = ?"
        ),
        value = 3,
        docs = """
          The predicate to `Table.update` is mandatory, to avoid anyone forgetting to
          provide one and accidentally bulk-updating all rows in their table. If you
          really do want to update all rows in the table, you can provide the predicate `_ => true`
        """
      )

      checker(
        query = Text {
          Buyer.select.filter(_.name `=` "James Bond").map(_.dateOfBirth).single
        },
        value = LocalDate.parse("2019-04-07")
      )
      checker(
        query = Text {
          Buyer.select.filter(_.name `=` "Li Haoyi").map(_.dateOfBirth).single
        },
        value = LocalDate.parse("2019-04-07")
      )
    }

    test("multiple") - {
      checker(
        query = Text {
          Buyer
            .update(_.name `=` "James Bond")
            .set(_.dateOfBirth := LocalDate.parse("2019-04-07"), _.name := "John Dee")
        },
        sqls = Seq(
          "UPDATE buyer SET date_of_birth = ?, name = ? WHERE (buyer.name = ?)",
          "UPDATE buyer SET buyer.date_of_birth = ?, buyer.name = ? WHERE (buyer.name = ?)"
        ),
        value = 1,
        docs = """
          This example shows how to update multiple columns in a single `Table.update` call
        """
      )

      checker(
        query = Text { Buyer.select.filter(_.name `=` "James Bond").map(_.dateOfBirth) },
        value = Seq[LocalDate]( /* not found due to rename */ )
      )

      checker(
        query = Text { Buyer.select.filter(_.name `=` "John Dee").map(_.dateOfBirth) },
        value = Seq(LocalDate.parse("2019-04-07"))
      )
    }

    test("dynamic") - {
      checker(
        query = Text {
          Buyer.update(_.name `=` "James Bond").set(c => c.name := c.name.toUpperCase)
        },
        sqls = Seq(
          "UPDATE buyer SET name = UPPER(buyer.name) WHERE (buyer.name = ?)",
          "UPDATE buyer SET buyer.name = UPPER(buyer.name) WHERE (buyer.name = ?)"
        ),
        value = 1,
        docs = """
          The values assigned to columns in `Table.update` can also be computed `Expr[T]`s,
          not just literal Scala constants. This example shows how to to update the name of
          the row for `James Bond` with it's existing name in uppercase
        """
      )

      checker(
        query = Text { Buyer.select.filter(_.name `=` "James Bond").map(_.dateOfBirth) },
        value = Seq[LocalDate]( /* not found due to rename */ )
      )

      checker(
        query = Text { Buyer.select.filter(_.name `=` "JAMES BOND").map(_.dateOfBirth) },
        value = Seq(LocalDate.parse("2001-02-03"))
      )
    }

  }
}
