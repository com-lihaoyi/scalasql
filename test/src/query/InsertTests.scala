package query

import usql.ExprOps._
import usql._
import utest._

/**
 * Tests for basic update operations
 */
object InsertTests extends TestSuite {
  def tests = Tests {
    val checker = new TestDb("querytests")
    test("simple") - {
      checker(
        Buyer.insert.values(
          _.name -> "test buyer",
          _.dateOfBirth -> "2023-09-09",
          _.id -> 4
        )
      ).expect(
          sql = """
            INSERT INTO buyer (name, date_of_birth, id)
            VALUES (?, ?, ?)
          """,
          value = 1
        )

      checker(Buyer.select.filter(_.name === "test buyer")).expect(
        value = Vector(Buyer(4, "test buyer", "2023-09-09"))
      )
    }

    test("batch") - {
      checker(
        Buyer.insert.batched(_.name, _.dateOfBirth, _.id)(
          ("test buyer A", "2001-04-07", 4),
          ("test buyer B", "2002-05-08", 5),
          ("test buyer C", "2003-06-09", 6)
        )
      ).expect(
          sql =
            """
            INSERT INTO buyer (name, date_of_birth, id)
            VALUES
              (?, ?, ?),
              (?, ?, ?),
              (?, ?, ?)
          """,
          value = 3
        )

      checker(Buyer.select).expect(
        value = Vector(
          Buyer(1, "James Bond", "2001-02-03"),
          Buyer(2, "叉烧包", "1923-11-12"),
          Buyer(3, "Li Haoyi", "1965-08-09"),
          Buyer(4, "test buyer A", "2001-04-07"),
          Buyer(5, "test buyer B", "2002-05-08"),
          Buyer(6, "test buyer C", "2003-06-09")
        )
      )
    }
  }
}

