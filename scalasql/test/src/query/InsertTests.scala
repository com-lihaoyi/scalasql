package scalasql.query

import scalasql._
import utest._
import utils.ScalaSqlSuite

import java.time.LocalDate

trait InsertTests extends ScalaSqlSuite {
  def description = "Basic `INSERT` operations"
  override def utestBeforeEach(path: Seq[String]): Unit = checker.reset()
  def tests = Tests {
    test("single") {
      test("values") - {
        checker(
          query = Buyer.insert.values(
            Buyer[Sc](4, "test buyer", LocalDate.parse("2023-09-09"))
          ),
          sql = "INSERT INTO buyer (id, name, date_of_birth) VALUES (?, ?, ?)",
          value = 1,
          docs = """
            `Table.insert.values` with a single value inserts a single row into the given table
          """
        )

        checker(
          query = Buyer.select.filter(_.name `=` "test buyer"),
          value = Seq(Buyer[Sc](4, "test buyer", LocalDate.parse("2023-09-09")))
        )
      }
      test("skipped") - {
        checker(
          query = Buyer.insert
            .values(
              Buyer[Sc](-1, "test buyer", LocalDate.parse("2023-09-09"))
            )
            .skipColumns(_.id),
          sql = "INSERT INTO buyer (name, date_of_birth) VALUES (?, ?)",
          value = 1,
          docs = """
            You can pass in one or more columns to `.skipColumns` to avoid inserting them.
            This is useful for columns where you want to rely on the value being
            auto-generated by the database.
          """
        )

        checker(
          query = Buyer.select.filter(_.name `=` "test buyer"),
          value = Seq(Buyer[Sc](4, "test buyer", LocalDate.parse("2023-09-09")))
        )
      }

      test("columns") - {
        checker(
          query = Buyer.insert.columns(
            _.name := "test buyer",
            _.dateOfBirth := LocalDate.parse("2023-09-09"),
            _.id := 4
          ),
          sql = "INSERT INTO buyer (name, date_of_birth, id) VALUES (?, ?, ?)",
          value = 1,
          docs = """
            `Table.insert.columns` inserts a single row into the given table, with the specified
             columns assigned to the given values, and any non-specified columns left `NULL`
             or assigned to their default values
          """
        )

        checker(
          query = Buyer.select.filter(_.name `=` "test buyer"),
          value = Seq(Buyer[Sc](4, "test buyer", LocalDate.parse("2023-09-09")))
        )
      }

      test("partial") - {
        checker(
          query = Buyer.insert
            .columns(_.name := "test buyer", _.dateOfBirth := LocalDate.parse("2023-09-09")),
          sql = "INSERT INTO buyer (name, date_of_birth) VALUES (?, ?)",
          value = 1
        )

        checker(
          query = Buyer.select.filter(_.name `=` "test buyer"),
          // id=4 comes from auto increment
          value = Seq(Buyer[Sc](4, "test buyer", LocalDate.parse("2023-09-09")))
        )
      }
    }

    test("conflict") - intercept[Exception] {
      checker(
        query = Buyer.insert.columns(
          _.name := "test buyer",
          _.dateOfBirth := LocalDate.parse("2023-09-09"),
          _.id := 1 // This should cause a primary key conflict
        ),
        value = 1
      )
    }

    test("batch") {
      test("values") - {
        checker(
          query = Buyer.insert.values(
            Buyer[Sc](4, "test buyer A", LocalDate.parse("2001-04-07")),
            Buyer[Sc](5, "test buyer B", LocalDate.parse("2002-05-08")),
            Buyer[Sc](6, "test buyer C", LocalDate.parse("2003-06-09"))
          ),
          sql = """
            INSERT INTO buyer (id, name, date_of_birth)
            VALUES (?, ?, ?), (?, ?, ?), (?, ?, ?)
          """,
          value = 3,
          docs = """
            You can insert multiple rows at once by passing them to `Buyer.insert.values`
          """
        )

        checker(
          query = Buyer.select,
          value = Seq(
            Buyer[Sc](1, "James Bond", LocalDate.parse("2001-02-03")),
            Buyer[Sc](2, "叉烧包", LocalDate.parse("1923-11-12")),
            Buyer[Sc](3, "Li Haoyi", LocalDate.parse("1965-08-09")),
            // id=4,5,6 comes from auto increment
            Buyer[Sc](4, "test buyer A", LocalDate.parse("2001-04-07")),
            Buyer[Sc](5, "test buyer B", LocalDate.parse("2002-05-08")),
            Buyer[Sc](6, "test buyer C", LocalDate.parse("2003-06-09"))
          )
        )
      }

      test("partial") - {
        checker(
          query = Buyer.insert.batched(_.name, _.dateOfBirth)(
            ("test buyer A", LocalDate.parse("2001-04-07")),
            ("test buyer B", LocalDate.parse("2002-05-08")),
            ("test buyer C", LocalDate.parse("2003-06-09"))
          ),
          sql = """
            INSERT INTO buyer (name, date_of_birth)
            VALUES (?, ?), (?, ?), (?, ?)
          """,
          value = 3
        )

        checker(
          query = Buyer.select,
          value = Seq(
            Buyer[Sc](1, "James Bond", LocalDate.parse("2001-02-03")),
            Buyer[Sc](2, "叉烧包", LocalDate.parse("1923-11-12")),
            Buyer[Sc](3, "Li Haoyi", LocalDate.parse("1965-08-09")),
            // id=4,5,6 comes from auto increment
            Buyer[Sc](4, "test buyer A", LocalDate.parse("2001-04-07")),
            Buyer[Sc](5, "test buyer B", LocalDate.parse("2002-05-08")),
            Buyer[Sc](6, "test buyer C", LocalDate.parse("2003-06-09"))
          )
        )
      }

    }

    test("select") {
      test("caseclass") {
        checker(
          query = Buyer.insert.select(
            identity,
            Buyer.select
              .filter(_.name <> "Li Haoyi")
              .map(b => b.copy(id = b.id + Buyer.select.maxBy(_.id)))
          ),
          sql = """
            INSERT INTO buyer (id, name, date_of_birth)
            SELECT
              (buyer0.id + (SELECT MAX(buyer1.id) AS res FROM buyer buyer1)) AS id,
              buyer0.name AS name,
              buyer0.date_of_birth AS date_of_birth
            FROM buyer buyer0
            WHERE (buyer0.name <> ?)
          """,
          value = 2,
          docs = """
            `Table.insert.select` inserts rows into the given table based on the given `Table.select`
            clause, and translates directly into SQL's `INSERT INTO ... SELECT` syntax.
          """
        )

        checker(
          query = Buyer.select,
          value = Seq(
            Buyer[Sc](1, "James Bond", LocalDate.parse("2001-02-03")),
            Buyer[Sc](2, "叉烧包", LocalDate.parse("1923-11-12")),
            Buyer[Sc](3, "Li Haoyi", LocalDate.parse("1965-08-09")),
            Buyer[Sc](4, "James Bond", LocalDate.parse("2001-02-03")),
            Buyer[Sc](5, "叉烧包", LocalDate.parse("1923-11-12"))
          )
        )
      }

      test("simple") {
        checker(
          query = Buyer.insert.select(
            x => (x.name, x.dateOfBirth),
            Buyer.select.map(x => (x.name, x.dateOfBirth)).filter(_._1 <> "Li Haoyi")
          ),
          sql = """
            INSERT INTO buyer (name, date_of_birth)
            SELECT buyer0.name AS res_0, buyer0.date_of_birth AS res_1
            FROM buyer buyer0
            WHERE (buyer0.name <> ?)
          """,
          value = 2
        )

        checker(
          query = Buyer.select,
          value = Seq(
            Buyer[Sc](1, "James Bond", LocalDate.parse("2001-02-03")),
            Buyer[Sc](2, "叉烧包", LocalDate.parse("1923-11-12")),
            Buyer[Sc](3, "Li Haoyi", LocalDate.parse("1965-08-09")),
            // id=4,5 comes from auto increment, 6 is filtered out in the select
            Buyer[Sc](4, "James Bond", LocalDate.parse("2001-02-03")),
            Buyer[Sc](5, "叉烧包", LocalDate.parse("1923-11-12"))
          )
        )
      }

    }
  }
}
