package query

import usql.ExprOps._
import usql._
import usql.query.Expr
import utest._

/**
 * Tests for basic insert operations
 */
object InsertTests extends TestSuite {
  def tests = Tests {
    val checker = new TestDb("querytests")
    test("single") {
      test("simple") - {
        checker(Buyer.insert.values(_.name -> "test buyer", _.dateOfBirth -> "2023-09-09", _.id -> 4))
          .expect(sql = "INSERT INTO buyer (name, date_of_birth, id) VALUES (?, ?, ?)", value = 1)

        checker(Buyer.select.filter(_.name === "test buyer")).expect(
          value = Vector(Buyer(4, "test buyer", "2023-09-09"))
        )
      }

      test("partial") - {
        checker(Buyer.insert.values(_.name -> "test buyer", _.dateOfBirth -> "2023-09-09"))
          .expect(sql = "INSERT INTO buyer (name, date_of_birth) VALUES (?, ?)", value = 1)

        checker(Buyer.select.filter(_.name === "test buyer")).expect(
          // id=4 comes from auto increment
          value = Vector(Buyer(4, "test buyer", "2023-09-09"))
        )
      }

      test("returning") - {
        checker(
          Buyer.insert.values(_.name -> "test buyer", _.dateOfBirth -> "2023-09-09").returning(_.id)
        ).expect(
          sql = "INSERT INTO buyer (name, date_of_birth) VALUES (?, ?) RETURNING buyer.id as res",
          value = Seq(4)
        )

        checker(Buyer.select.filter(_.name === "test buyer")).expect(
          // id=4 comes from auto increment
          value = Vector(Buyer(4, "test buyer", "2023-09-09"))
        )
      }
    }

    test("batch") {
      test("simple") - {
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

      test("partial") - {
        checker(
          Buyer.insert.batched(_.name, _.dateOfBirth)(
            ("test buyer A", "2001-04-07"),
            ("test buyer B", "2002-05-08"),
            ("test buyer C", "2003-06-09")
          )
        ).expect(
          sql = """
            INSERT INTO buyer (name, date_of_birth)
            VALUES
              (?, ?),
              (?, ?),
              (?, ?)
          """,
          value = 3
        )

        checker(Buyer.select).expect(
          value = Vector(
            Buyer(1, "James Bond", "2001-02-03"),
            Buyer(2, "叉烧包", "1923-11-12"),
            Buyer(3, "Li Haoyi", "1965-08-09"),
            // id=4,5,6 comes from auto increment
            Buyer(4, "test buyer A", "2001-04-07"),
            Buyer(5, "test buyer B", "2002-05-08"),
            Buyer(6, "test buyer C", "2003-06-09")
          )
        )
      }
      test("returning") - {
        checker(
          Buyer.insert.batched(_.name, _.dateOfBirth)(
            ("test buyer A", "2001-04-07"),
            ("test buyer B", "2002-05-08"),
            ("test buyer C", "2003-06-09")
          ).returning(_.id)
        ).expect(
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

        checker(Buyer.select).expect(
          value = Vector(
            Buyer(1, "James Bond", "2001-02-03"),
            Buyer(2, "叉烧包", "1923-11-12"),
            Buyer(3, "Li Haoyi", "1965-08-09"),
            // id=4,5,6 comes from auto increment
            Buyer(4, "test buyer A", "2001-04-07"),
            Buyer(5, "test buyer B", "2002-05-08"),
            Buyer(6, "test buyer C", "2003-06-09")
          )
        )
      }
    }
    test("select"){
      checker(
        Buyer.insert.select(
          x => (x.name, x.dateOfBirth),
          Buyer.select.map(x => (x.name, x.dateOfBirth)).filter(_._1 !== "Li Haoyi")
        )
      ).expect(
        sql = """
          INSERT INTO buyer (name, date_of_birth)
          SELECT
            buyer0.name as res__0,
            buyer0.date_of_birth as res__1
          FROM buyer buyer0
          WHERE buyer0.name <> ?
        """,
        value = 2
      )

      checker(Buyer.select).expect(
        value = Vector(
          Buyer(1, "James Bond", "2001-02-03"),
          Buyer(2, "叉烧包", "1923-11-12"),
          Buyer(3, "Li Haoyi", "1965-08-09"),
          // id=4,5 comes from auto increment, 6 is filtered out in the select
          Buyer(4, "James Bond", "2001-02-03"),
          Buyer(5, "叉烧包", "1923-11-12"),
        )
      )
    }
  }
}

