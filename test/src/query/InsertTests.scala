package scalasql.query

import scalasql._
import utest._

import java.time.LocalDate

/**
 * Tests for basic insert operations
 */
trait InsertTests extends ScalaSqlSuite {
  override def utestBeforeEach(path: Seq[String]): Unit = checker.reset()
  def tests = Tests {
    test("single") {
      test("simple") - {
        checker(
          query = Buyer.insert.values(
            _.name -> "test buyer",
            _.dateOfBirth -> LocalDate.parse("2023-09-09"),
            _.id -> 4
          ),
          sql = "INSERT INTO buyer (name, date_of_birth, id) VALUES (?, ?, ?)",
          value = 1
        )

        checker(
          query = Buyer.select.filter(_.name === "test buyer"),
          value = Seq(Buyer[Id](4, "test buyer", LocalDate.parse("2023-09-09")))
        )
      }

      test("partial") - {
        checker(
          query = Buyer.insert
            .values(_.name -> "test buyer", _.dateOfBirth -> LocalDate.parse("2023-09-09")),
          sql = "INSERT INTO buyer (name, date_of_birth) VALUES (?, ?)",
          value = 1
        )

        checker(
          query = Buyer.select.filter(_.name === "test buyer"),
          // id=4 comes from auto increment
          value = Seq(Buyer[Id](4, "test buyer", LocalDate.parse("2023-09-09")))
        )
      }
    }

    test("conflict") - intercept[Exception] {
      checker(
        query = Buyer.insert.values(
          _.name -> "test buyer",
          _.dateOfBirth -> LocalDate.parse("2023-09-09"),
          _.id -> 1 // This should cause a primary key conflict
        ),
        value = 1
      )
    }

    test("batch") {
      test("simple") - {
        checker(
          query = Buyer.insert.batched(_.name, _.dateOfBirth, _.id)(
            ("test buyer A", LocalDate.parse("2001-04-07"), 4),
            ("test buyer B", LocalDate.parse("2002-05-08"), 5),
            ("test buyer C", LocalDate.parse("2003-06-09"), 6)
          ),
          sql = """
            INSERT INTO buyer (name, date_of_birth, id)
            VALUES
              (?, ?, ?),
              (?, ?, ?),
              (?, ?, ?)
          """,
          value = 3
        )

        checker(
          query = Buyer.select,
          value = Seq(
            Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")),
            Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
            Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09")),
            Buyer[Id](4, "test buyer A", LocalDate.parse("2001-04-07")),
            Buyer[Id](5, "test buyer B", LocalDate.parse("2002-05-08")),
            Buyer[Id](6, "test buyer C", LocalDate.parse("2003-06-09"))
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
            Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")),
            Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
            Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09")),
            // id=4,5,6 comes from auto increment
            Buyer[Id](4, "test buyer A", LocalDate.parse("2001-04-07")),
            Buyer[Id](5, "test buyer B", LocalDate.parse("2002-05-08")),
            Buyer[Id](6, "test buyer C", LocalDate.parse("2003-06-09"))
          )
        )
      }

    }

    test("select") {
      test("caseclass") {
        checker(
          query = Buyer.insert.select(
            identity,
            Buyer.select.filter(_.name !== "Li Haoyi")
              .map(b => b.copy(id = b.id + Buyer.select.maxBy(_.id)))
          ),
          sql = """
            INSERT INTO buyer (id, name, date_of_birth)
            SELECT
              buyer0.id + (SELECT MAX(buyer0.id) as res FROM buyer buyer0) as res__id,
              buyer0.name as res__name,
              buyer0.date_of_birth as res__date_of_birth
            FROM buyer buyer0
            WHERE buyer0.name <> ?
          """,
          value = 2
        )

        checker(
          query = Buyer.select,
          value = Seq(
            Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")),
            Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
            Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09")),
            Buyer[Id](4, "James Bond", LocalDate.parse("2001-02-03")),
            Buyer[Id](5, "叉烧包", LocalDate.parse("1923-11-12"))
          )
        )
      }

      test("simple") {
        checker(
          query = Buyer.insert.select(
            x => (x.name, x.dateOfBirth),
            Buyer.select.map(x => (x.name, x.dateOfBirth)).filter(_._1 !== "Li Haoyi")
          ),
          sql = """
            INSERT INTO buyer (name, date_of_birth)
            SELECT buyer0.name as res__0, buyer0.date_of_birth as res__1
            FROM buyer buyer0
            WHERE buyer0.name <> ?
          """,
          value = 2
        )

        checker(
          query = Buyer.select,
          value = Seq(
            Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")),
            Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
            Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09")),
            // id=4,5 comes from auto increment, 6 is filtered out in the select
            Buyer[Id](4, "James Bond", LocalDate.parse("2001-02-03")),
            Buyer[Id](5, "叉烧包", LocalDate.parse("1923-11-12"))
          )
        )
      }

    }
  }
}
