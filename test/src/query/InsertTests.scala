package usql.query

import usql.ExprOps._
import usql._
import usql.query.Expr
import utest._

import java.sql.Date


object PostgresInsertTests extends InsertTests with PostgresSuite
object SqliteInsertTests extends InsertTests with SqliteSuite
object MySqlInsertTests extends InsertTests with MySqlSuite
/**
 * Tests for basic insert operations
 */
trait InsertTests extends TestSuite {
  val checker: TestDb

  override def utestBeforeEach(path: Seq[String]): Unit = checker.reset()
  def tests = Tests {
    test("single") {
      test("simple") - {
        checker(
          query =
            Buyer.insert.values(_.name -> "test buyer", _.dateOfBirth -> Date.valueOf("2023-09-09"), _.id -> 4),
          sql = "INSERT INTO buyer (name, date_of_birth, id) VALUES (?, ?, ?)",
          value = 1
        )

        checker(
          query = Buyer.select.filter(_.name === "test buyer"),
          value = Seq(Buyer[Val](4, "test buyer", Date.valueOf("2023-09-09")))
        )
      }

      test("partial") - {
        checker(
          query = Buyer.insert.values(_.name -> "test buyer", _.dateOfBirth -> Date.valueOf("2023-09-09")),
          sql = "INSERT INTO buyer (name, date_of_birth) VALUES (?, ?)",
          value = 1
        )

        checker(
          query = Buyer.select.filter(_.name === "test buyer"),
          // id=4 comes from auto increment
          value = Seq(Buyer[Val](4, "test buyer", Date.valueOf("2023-09-09")))
        )
      }

      test("returning") - {
        checker(
          query = Buyer.insert
            .values(_.name -> "test buyer", _.dateOfBirth -> Date.valueOf("2023-09-09"))
            .returning(_.id),
          sql = "INSERT INTO buyer (name, date_of_birth) VALUES (?, ?) RETURNING buyer.id as res",
          value = Seq(4)
        )

        checker(
          query = Buyer.select.filter(_.name === "test buyer"),
          // id=4 comes from auto increment
          value = Seq(Buyer[Val](4, "test buyer", Date.valueOf("2023-09-09")))
        )
      }
    }

    test("batch") {
      test("simple") - {
        checker(
          query = Buyer.insert.batched(_.name, _.dateOfBirth, _.id)(
            ("test buyer A", Date.valueOf("2001-04-07"), 4),
            ("test buyer B", Date.valueOf("2002-05-08"), 5),
            ("test buyer C", Date.valueOf("2003-06-09"), 6)
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
            Buyer[Val](1, "James Bond", Date.valueOf("2001-02-03")),
            Buyer[Val](2, "叉烧包", Date.valueOf("1923-11-12")),
            Buyer[Val](3, "Li Haoyi", Date.valueOf("1965-08-09")),
            Buyer[Val](4, "test buyer A", Date.valueOf("2001-04-07")),
            Buyer[Val](5, "test buyer B", Date.valueOf("2002-05-08")),
            Buyer[Val](6, "test buyer C", Date.valueOf("2003-06-09"))
          )
        )
      }

      test("partial") - {
        checker(
          query = Buyer.insert.batched(_.name, _.dateOfBirth)(
            ("test buyer A", Date.valueOf("2001-04-07")),
            ("test buyer B", Date.valueOf("2002-05-08")),
            ("test buyer C", Date.valueOf("2003-06-09"))
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
            Buyer[Val](1, "James Bond", Date.valueOf("2001-02-03")),
            Buyer[Val](2, "叉烧包", Date.valueOf("1923-11-12")),
            Buyer[Val](3, "Li Haoyi", Date.valueOf("1965-08-09")),
            // id=4,5,6 comes from auto increment
            Buyer[Val](4, "test buyer A", Date.valueOf("2001-04-07")),
            Buyer[Val](5, "test buyer B", Date.valueOf("2002-05-08")),
            Buyer[Val](6, "test buyer C", Date.valueOf("2003-06-09"))
          )
        )
      }

      test("returning") - {
        checker(
          query = Buyer.insert
            .batched(_.name, _.dateOfBirth)(
              ("test buyer A", Date.valueOf("2001-04-07")),
              ("test buyer B", Date.valueOf("2002-05-08")),
              ("test buyer C", Date.valueOf("2003-06-09"))
            )
            .returning(_.id),
          sql = """
            INSERT INTO buyer (name, date_of_birth)
            VALUES
              (?, ?),
              (?, ?),
              (?, ?)
            RETURNING buyer.id as res
          """,
          value = Seq(4, 5, 6)
        )

        checker(
          query = Buyer.select,
          value = Seq(
            Buyer[Val](1, "James Bond", Date.valueOf("2001-02-03")),
            Buyer[Val](2, "叉烧包", Date.valueOf("1923-11-12")),
            Buyer[Val](3, "Li Haoyi", Date.valueOf("1965-08-09")),
            // id=4,5,6 comes from auto increment
            Buyer[Val](4, "test buyer A", Date.valueOf("2001-04-07")),
            Buyer[Val](5, "test buyer B", Date.valueOf("2002-05-08")),
            Buyer[Val](6, "test buyer C", Date.valueOf("2003-06-09"))
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
              .filter(_.name !== "Li Haoyi")
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
            Buyer[Val](1, "James Bond", Date.valueOf("2001-02-03")),
            Buyer[Val](2, "叉烧包", Date.valueOf("1923-11-12")),
            Buyer[Val](3, "Li Haoyi", Date.valueOf("1965-08-09")),
            Buyer[Val](4, "James Bond", Date.valueOf("2001-02-03")),
            Buyer[Val](5, "叉烧包", Date.valueOf("1923-11-12")),
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
            Buyer[Val](1, "James Bond", Date.valueOf("2001-02-03")),
            Buyer[Val](2, "叉烧包", Date.valueOf("1923-11-12")),
            Buyer[Val](3, "Li Haoyi", Date.valueOf("1965-08-09")),
            // id=4,5 comes from auto increment, 6 is filtered out in the select
            Buyer[Val](4, "James Bond", Date.valueOf("2001-02-03")),
            Buyer[Val](5, "叉烧包", Date.valueOf("1923-11-12"))
          )
        )
      }

      test("returning") {
        checker(
          query = Buyer.insert
            .select(
              x => (x.name, x.dateOfBirth),
              Buyer.select.map(x => (x.name, x.dateOfBirth)).filter(_._1 !== "Li Haoyi")
            )
            .returning(_.id),
          sql = """
            INSERT INTO buyer (name, date_of_birth)
            SELECT
              buyer0.name as res__0,
              buyer0.date_of_birth as res__1
            FROM buyer buyer0
            WHERE buyer0.name <> ?
            RETURNING buyer.id as res
          """,
          value = Seq(4, 5)
        )

        checker(
          query = Buyer.select,
          value = Seq(
            Buyer[Val](1, "James Bond", Date.valueOf("2001-02-03")),
            Buyer[Val](2, "叉烧包", Date.valueOf("1923-11-12")),
            Buyer[Val](3, "Li Haoyi", Date.valueOf("1965-08-09")),
            // id=4,5 comes from auto increment, 6 is filtered out in the select
            Buyer[Val](4, "James Bond", Date.valueOf("2001-02-03")),
            Buyer[Val](5, "叉烧包", Date.valueOf("1923-11-12"))
          )
        )
      }
    }
  }
}
