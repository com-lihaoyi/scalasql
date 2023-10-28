package scalasql.query

import scalasql._
import utest._

import java.time.LocalDate

/**
 * Tests for basic insert operations
 */
trait OnConflictTests extends ScalaSqlSuite {
  override def utestBeforeEach(path: Seq[String]): Unit = checker.reset()
  def tests = Tests {
    test("conflict") {

      test("ignore") - {

        checker(
          query =
            Buyer.insert.values(
              _.name -> "test buyer",
              _.dateOfBirth -> LocalDate.parse("2023-09-09"),
              _.id -> 1 // This should cause a primary key conflict
            ).onConflictIgnore(_.id),
          sqls = Seq(
            "INSERT INTO buyer (name, date_of_birth, id) VALUES (?, ?, ?) ON CONFLICT (id) DO NOTHING",

            // H2, only works under MODE=PostgreSQL;
            "INSERT INTO buyer (name, date_of_birth, id) VALUES (?, ?, ?) ON CONFLICT DO NOTHING",
            // MySql
            "INSERT INTO buyer (name, date_of_birth, id) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE id = id"
          ),
          value = 0,
          moreValues = Seq(1) // returns 1 in mysql
        )
      }
    }
  }
}
