package usql.query

import usql._
import utest._

import java.sql.Date

/**
 * Tests for basic update operations
 */
trait ReturningTests extends UsqlTestSuite {
  override def utestBeforeEach(path: Seq[String]): Unit = checker.reset()
  def tests = Tests {
    test("insert") {
      test("single") - {
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

      test("multiple") - {
        checker(
          query = Buyer.insert
            .batched(_.name, _.dateOfBirth)(
              ("test buyer A", Date.valueOf("2001-04-07")),
              ("test buyer B", Date.valueOf("2002-05-08")),
              ("test buyer C", Date.valueOf("2003-06-09"))
            )
            .returning(_.id),
          sql =
            """
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

      test("select") {
        checker(
          query = Buyer.insert
            .select(
              x => (x.name, x.dateOfBirth),
              Buyer.select.map(x => (x.name, x.dateOfBirth)).filter(_._1 !== "Li Haoyi")
            )
            .returning(_.id),
          sql =
            """
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

    test("update") {
      test("single") - {
        checker(
          query =
            Buyer.update
              .filter(_.name === "James Bond")
              .set(_.dateOfBirth -> Date.valueOf("2019-04-07"))
              .returning(_.id),
          sqls = Seq(
            "UPDATE buyer SET date_of_birth = ? WHERE buyer.name = ? RETURNING buyer.id as res",
            "UPDATE buyer SET buyer.date_of_birth = ? WHERE buyer.name = ? RETURNING buyer.id as res",
          ),
          value = Seq(1)
        )

        checker(
          query = Buyer.select.filter(_.name === "James Bond").map(_.dateOfBirth),
          value = Seq(Date.valueOf("2019-04-07"))
        )
      }

      test("multiple") - {
        checker(
          query =
            Buyer.update
              .filter(_.name === "James Bond")
              .set(_.dateOfBirth -> Date.valueOf("2019-04-07"), _.name -> "John Dee")
              .returning(c => (c.id, c.name, c.dateOfBirth)),
          sqls = Seq(
            """
            UPDATE buyer
            SET date_of_birth = ?, name = ? WHERE buyer.name = ?
            RETURNING buyer.id as res__0, buyer.name as res__1, buyer.date_of_birth as res__2
          """,
            """
            UPDATE buyer
            SET buyer.date_of_birth = ?, buyer.name = ? WHERE buyer.name = ?
            RETURNING buyer.id as res__0, buyer.name as res__1, buyer.date_of_birth as res__2
          """
          ),
          value = Seq((1, "John Dee", Date.valueOf("2019-04-07")))
        )
      }
    }
  }
}
