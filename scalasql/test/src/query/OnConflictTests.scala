package scalasql.query

import scalasql._
import scalasql.dialects.{OnConflictOps, ReturningDialect}
import sourcecode.Text
import utest._
import utils.ScalaSqlSuite

import java.time.LocalDate

trait OnConflictTests extends ScalaSqlSuite {
  this: OnConflictOps & ReturningDialect =>
  def description = "Queries using `ON CONFLICT DO UPDATE` or `ON CONFLICT DO NOTHING`"
  override def utestBeforeEach(path: Seq[String]): Unit = checker.reset()
  def tests = Tests {
    
    test("t") - {
      checker(
        query = Text {
          Buyer.insert.values(Buyer[Sc](
            id = 1,
            name = "t",
            dateOfBirth = LocalDate.now()
          )).onConflictIgnore(_.id)
        },
        sql =
          "INSERT INTO buyer (id, name, date_of_birth) VALUES (?, ?, ?) ON CONFLICT (id) DO NOTHING",
        value = 0,
        docs = """
          ScalaSql's `.onConflictIgnore` translates into SQL's `ON CONFLICT DO NOTHING`

          Note that H2 and HsqlExpr do not support `onConflictIgnore` and `onConflictUpdate`, while
          MySql only supports `onConflictUpdate` but not `onConflictIgnore`.
        """
      )
    }

    test("ignore") - {
      checker(
        query = Text {
          Buyer.insert
            .columns(
              _.name := "test buyer",
              _.dateOfBirth := LocalDate.parse("2023-09-09"),
              _.id := 1 // This should cause a primary key conflict
            )
            .onConflictIgnore(_.id)
        },
        sql =
          "INSERT INTO buyer (name, date_of_birth, id) VALUES (?, ?, ?) ON CONFLICT (id) DO NOTHING",
        value = 0,
        docs = """
          ScalaSql's `.onConflictIgnore` translates into SQL's `ON CONFLICT DO NOTHING`

          Note that H2 and HsqlExpr do not support `onConflictIgnore` and `onConflictUpdate`, while
          MySql only supports `onConflictUpdate` but not `onConflictIgnore`.
        """
      )

      test("returningEmpty") - {
        checker(
          query = Text {
            Buyer.insert
              .columns(
                _.name := "test buyer",
                _.dateOfBirth := LocalDate.parse("2023-09-09"),
                _.id := 1 // This should cause a primary key conflict
              )
              .onConflictIgnore(_.id)
              .returning(_.name)
          },
          sql = """
            INSERT INTO buyer (name, date_of_birth, id) VALUES (?, ?, ?)
            ON CONFLICT (id) DO NOTHING
            RETURNING buyer.name AS res
          """,
          value = Seq.empty[String]
        )
      }

      test("returningOne") - {
        checker(
          query = Text {
            Buyer.insert
              .columns(
                _.name := "test buyer",
                _.dateOfBirth := LocalDate.parse("2023-09-09"),
                _.id := 4 // This should cause a primary key conflict
              )
              .onConflictIgnore(_.id)
              .returning(_.name)
          },
          sql = """
            INSERT INTO buyer (name, date_of_birth, id) VALUES (?, ?, ?)
            ON CONFLICT (id) DO NOTHING
            RETURNING buyer.name AS res
          """,
          value = Seq("test buyer")
        )
      }

    }

    test("update") - {
      checker(
        query = Text {
          Buyer.insert
            .columns(
              _.name := "test buyer",
              _.dateOfBirth := LocalDate.parse("2023-09-09"),
              _.id := 1 // This should cause a primary key conflict
            )
            .onConflictUpdate(_.id)(_.name := "TEST BUYER CONFLICT")
        },
        sql =
          "INSERT INTO buyer (name, date_of_birth, id) VALUES (?, ?, ?) ON CONFLICT (id) DO UPDATE SET name = ?",
        value = 1,
        docs = """
          ScalaSql's `.onConflictUpdate` translates into SQL's `ON CONFLICT DO UPDATE`
        """
      )

      checker(
        query = Text { Buyer.select },
        value = Seq(
          Buyer[Sc](1, "TEST BUYER CONFLICT", LocalDate.parse("2001-02-03")),
          Buyer[Sc](2, "叉烧包", LocalDate.parse("1923-11-12")),
          Buyer[Sc](3, "Li Haoyi", LocalDate.parse("1965-08-09"))
        ),
        normalize = (x: Seq[Buyer[Sc]]) => x.sortBy(_.id)
      )
    }

    test("computed") - {
      checker(
        query = Text {
          Buyer.insert
            .columns(
              _.name := "test buyer",
              _.dateOfBirth := LocalDate.parse("2023-09-09"),
              _.id := 1 // This should cause a primary key conflict
            )
            .onConflictUpdate(_.id)(v => v.name := v.name.toUpperCase)
        },
        sql =
          "INSERT INTO buyer (name, date_of_birth, id) VALUES (?, ?, ?) ON CONFLICT (id) DO UPDATE SET name = UPPER(buyer.name)",
        value = 1
      )

      checker(
        query = Text { Buyer.select },
        value = Seq(
          Buyer[Sc](1, "JAMES BOND", LocalDate.parse("2001-02-03")),
          Buyer[Sc](2, "叉烧包", LocalDate.parse("1923-11-12")),
          Buyer[Sc](3, "Li Haoyi", LocalDate.parse("1965-08-09"))
        ),
        normalize = (x: Seq[Buyer[Sc]]) => x.sortBy(_.id)
      )
    }

    test("returning") - {
      checker(
        query = Text {
          Buyer.insert
            .columns(
              _.name := "test buyer",
              _.dateOfBirth := LocalDate.parse("2023-09-09"),
              _.id := 1 // This should cause a primary key conflict
            )
            .onConflictUpdate(_.id)(v => v.name := v.name.toUpperCase)
            .returning(_.name)
            .single
        },
        sql = """
          INSERT INTO buyer (name, date_of_birth, id) VALUES (?, ?, ?)
          ON CONFLICT (id) DO UPDATE
          SET name = UPPER(buyer.name)
          RETURNING buyer.name AS res
        """,
        value = "JAMES BOND"
      )
    }
  }
}
