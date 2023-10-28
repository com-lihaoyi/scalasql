package scalasql.dialects

import scalasql._
import scalasql.query.Expr
import utest._

import java.time.LocalDate

trait MySqlDialectTests extends MySqlSuite {
  def tests = Tests {
    test("reverse") - checker(
      query = Expr("Hello").reverse,
      sql = "SELECT REVERSE(?) as res",
      value = "olleH"
    )

    test("lpad") - checker(
      query = Expr("Hello").lpad(10, "xy"),
      sql = "SELECT LPAD(?, ?, ?) as res",
      value = "xyxyxHello"
    )

    test("rpad") - checker(
      query = Expr("Hello").rpad(10, "xy"),
      sql = "SELECT RPAD(?, ?, ?) as res",
      value = "Helloxyxyx"
    )

    test("conflict") {

      test("ignore") - {

        checker(
          query =
            Buyer.insert.values(
              _.name -> "test buyer",
              _.dateOfBirth -> LocalDate.parse("2023-09-09"),
              _.id -> 1 // This should cause a primary key conflict
            ).onConflictUpdate(x => x.id -> x.id),
          sql = "INSERT INTO buyer (name, date_of_birth, id) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE id = id",
          value = 1,
        )
      }

      test("update") - {
        checker(
          query =
            Buyer.insert.values(
              _.name -> "test buyer",
              _.dateOfBirth -> LocalDate.parse("2023-09-09"),
              _.id -> 1 // This should cause a primary key conflict
            ).onConflictUpdate(_.name -> "TEST BUYER CONFLICT"),
          sql = "INSERT INTO buyer (name, date_of_birth, id) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE name = ?",
          value = 2
        )

        checker(
          query = Buyer.select,
          value = Seq(
            Buyer[Id](1, "TEST BUYER CONFLICT", LocalDate.parse("2001-02-03")),
            Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
            Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09")),
          ),
          normalize = (x: Seq[Buyer[Id]]) => x.sortBy(_.id)
        )
      }
    }

  }
}
