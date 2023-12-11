package scalasql.query

import scalasql._
import scalasql.dialects.ReturningDialect
import sourcecode.Text
import utest._
import utils.ScalaSqlSuite

import java.time.LocalDate

/**
 * Tests for basic update operations
 */
trait ReturningTests extends ScalaSqlSuite {
  this: ReturningDialect =>
  def description = "Queries using `INSERT` or `UPDATE` with `RETURNING`"
  override def utestBeforeEach(path: Seq[String]): Unit = checker.reset()
  def tests = Tests {
    test("insert") {
      test("single") - {
        checker(
          query = Text {
            Buyer.insert
              .columns(_.name := "test buyer", _.dateOfBirth := LocalDate.parse("2023-09-09"))
              .returning(_.id)
          },
          sql = "INSERT INTO buyer (name, date_of_birth) VALUES (?, ?) RETURNING buyer.id AS res",
          value = Seq(4),
          docs = """
            ScalaSql's `.returning` clause translates to SQL's `RETURNING` syntax, letting
            you perform insertions or updates and return values from the query (rather than
            returning a single integer representing the rows affected). This is especially
            useful for retrieving the auto-generated table IDs that many databases support.

            Note that `.returning`/`RETURNING` is not supported in MySql, H2 or HsqlDB
          """
        )

        checker(
          query = Text { Buyer.select.filter(_.name `=` "test buyer") },
          // id=4 comes from auto increment
          value = Seq(Buyer[Sc](4, "test buyer", LocalDate.parse("2023-09-09")))
        )
      }

      test("dotSingle") - {
        checker(
          query = Text {
            Buyer.insert
              .columns(_.name := "test buyer", _.dateOfBirth := LocalDate.parse("2023-09-09"))
              .returning(_.id)
              .single
          },
          sql = "INSERT INTO buyer (name, date_of_birth) VALUES (?, ?) RETURNING buyer.id AS res",
          value = 4,
          docs = """
            If your `.returning` query is expected to be a single row, the `.single` method is
            supported to convert the returned `Seq[T]` into a single `T`. `.single` throws an
            exception if zero or multiple rows are returned.
          """
        )

        checker(
          query = Text { Buyer.select.filter(_.name `=` "test buyer") },
          // id=4 comes from auto increment
          value = Seq(Buyer[Sc](4, "test buyer", LocalDate.parse("2023-09-09")))
        )
      }

      test("multiple") - {
        checker(
          query = Text {
            Buyer.insert
              .batched(_.name, _.dateOfBirth)(
                ("test buyer A", LocalDate.parse("2001-04-07")),
                ("test buyer B", LocalDate.parse("2002-05-08")),
                ("test buyer C", LocalDate.parse("2003-06-09"))
              )
              .returning(_.id)
          },
          sql = """
            INSERT INTO buyer (name, date_of_birth)
            VALUES
              (?, ?),
              (?, ?),
              (?, ?)
            RETURNING buyer.id AS res
          """,
          value = Seq(4, 5, 6)
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

      test("select") {
        checker(
          query = Text {
            Buyer.insert
              .select(
                x => (x.name, x.dateOfBirth),
                Buyer.select.map(x => (x.name, x.dateOfBirth)).filter(_._1 <> "Li Haoyi")
              )
              .returning(_.id)
          },
          sql = """
            INSERT INTO buyer (name, date_of_birth)
            SELECT
              buyer1.name AS res_0,
              buyer1.date_of_birth AS res_1
            FROM buyer buyer1
            WHERE (buyer1.name <> ?)
            RETURNING buyer.id AS res
          """,
          value = Seq(4, 5),
          docs = """
            All variants of `.insert` and `.update` support `.returning`, e.g. the example below
            applies to `.insert.select`, and the examples further down demonstrate its usage with
            `.update` and `.delete`
          """
        )

        checker(
          query = Text { Buyer.select },
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

    test("update") {
      test("single") - {
        checker(
          query = Text {
            Buyer
              .update(_.name `=` "James Bond")
              .set(_.dateOfBirth := LocalDate.parse("2019-04-07"))
              .returning(_.id)
          },
          sqls = Seq(
            "UPDATE buyer SET date_of_birth = ? WHERE (buyer.name = ?) RETURNING buyer.id AS res",
            "UPDATE buyer SET buyer.date_of_birth = ? WHERE (buyer.name = ?) RETURNING buyer.id AS res"
          ),
          value = Seq(1)
        )

        checker(
          query = Text { Buyer.select.filter(_.name `=` "James Bond").map(_.dateOfBirth) },
          value = Seq(LocalDate.parse("2019-04-07"))
        )
      }

      test("multiple") - {
        checker(
          query = Text {
            Buyer
              .update(_.name `=` "James Bond")
              .set(_.dateOfBirth := LocalDate.parse("2019-04-07"), _.name := "John Dee")
              .returning(c => (c.id, c.name, c.dateOfBirth))
          },
          sqls = Seq(
            """
            UPDATE buyer
            SET date_of_birth = ?, name = ? WHERE (buyer.name = ?)
            RETURNING buyer.id AS res_0, buyer.name AS res_1, buyer.date_of_birth AS res_2
          """,
            """
            UPDATE buyer
            SET buyer.date_of_birth = ?, buyer.name = ? (WHERE buyer.name = ?)
            RETURNING buyer.id AS res_0, buyer.name AS res_1, buyer.date_of_birth AS res_2
          """
          ),
          value = Seq((1, "John Dee", LocalDate.parse("2019-04-07")))
        )
      }
    }

    test("delete") {
      checker(
        query = Text { Purchase.delete(_.shippingInfoId `=` 1).returning(_.total) },
        sqls = Seq(
          "DELETE FROM purchase WHERE (purchase.shipping_info_id = ?) RETURNING purchase.total AS res"
        ),
        value = Seq(888.0, 900.0, 15.7)
      )

      checker(
        query = Purchase.select,
        value = Seq(
          // id=1,2,3 had shippingInfoId=1 and thus got deleted
          Purchase[Sc](id = 4, shippingInfoId = 2, productId = 4, count = 4, total = 493.8),
          Purchase[Sc](id = 5, shippingInfoId = 2, productId = 5, count = 10, total = 10000.0),
          Purchase[Sc](id = 6, shippingInfoId = 3, productId = 1, count = 5, total = 44.4),
          Purchase[Sc](id = 7, shippingInfoId = 3, productId = 6, count = 13, total = 1.3)
        )
      )
    }
  }
}
