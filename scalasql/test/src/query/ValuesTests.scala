package scalasql.query

import scalasql._
import sourcecode.Text
import utest._
import utils.ScalaSqlSuite

import java.time.LocalDate

trait ValuesTests extends ScalaSqlSuite {
  def description = "Basic `VALUES` operations"

  def tests = Tests {
    test("basic") - checker(
      query = Text { db.values(Seq(1, 2, 3)) },
      sqls = Seq("VALUES (?), (?), (?)", "VALUES ROW(?), ROW(?), ROW(?)"),
      value = Seq(1, 2, 3),
      docs = """
        You can use `Values` to generate a SQL `VALUES` clause
      """
    )

    test("contains") - checker(
      query = Text { db.values(Seq(1, 2, 3)).contains(1) },
      sqls = Seq(
        "SELECT (? IN (VALUES (?), (?), (?))) AS res",
        "SELECT (? IN (VALUES ROW(?), ROW(?), ROW(?))) AS res"
      ),
      value = true,
      docs = """
        `Values` supports `.contains`
      """
    )

    test("max") - checker(
      query = Text { db.values(Seq(1, 2, 3)).max },
      sqls = Seq(
        "SELECT MAX(subquery0.column1) AS res FROM (VALUES (?), (?), (?)) subquery0",
        "SELECT MAX(subquery0.c1) AS res FROM (VALUES (?), (?), (?)) subquery0",
        "SELECT MAX(subquery0.column_0) AS res FROM (VALUES ROW(?), ROW(?), ROW(?)) subquery0"
      ),
      value = 3,
      docs = """
        `Values` supports aggregate functions like `.max`
      """
    )

    test("map") - checker(
      query = Text { db.values(Seq(1, 2, 3)).map(_ + 1) },
      sqls = Seq(
        "SELECT (subquery0.column1 + ?) AS res FROM (VALUES (?), (?), (?)) subquery0",
        "SELECT (subquery0.c1 + ?) AS res FROM (VALUES (?), (?), (?)) subquery0",
        "SELECT (subquery0.column_0 + ?) AS res FROM (VALUES ROW(?), ROW(?), ROW(?)) subquery0"
      ),
      value = Seq(2, 3, 4),
      docs = """
        `Values` supports most `.select` operators like `.map`, `.filter`, `.crossJoin`, and so on
      """
    )

    test("filter") - checker(
      query = Text { db.values(Seq(1, 2, 3)).filter(_ > 2) },
      sqls = Seq(
        "SELECT subquery0.column1 AS res FROM (VALUES (?), (?), (?)) subquery0 WHERE (subquery0.column1 > ?)",
        "SELECT subquery0.c1 AS res FROM (VALUES (?), (?), (?)) subquery0 WHERE (subquery0.c1 > ?)",
        "SELECT subquery0.column_0 AS res FROM (VALUES ROW(?), ROW(?), ROW(?)) subquery0 WHERE (subquery0.column_0 > ?)"
      ),
      value = Seq(3),
      docs = ""
    )

    test("crossJoin") - checker(
      query = Text {
        db.values(Seq(1, 2, 3)).crossJoin(db.values(Seq(4, 5, 6))).map {
          case (a, b) => (a * 10 + b)
        }
      },
      sqls = Seq(
        """
        SELECT ((subquery0.column1 * ?) + subquery1.column1) AS res
        FROM (VALUES (?), (?), (?)) subquery0
        CROSS JOIN (VALUES (?), (?), (?)) subquery1
        """,
        """
        SELECT ((subquery0.c1 * ?) + subquery1.c1) AS res
        FROM (VALUES (?), (?), (?)) subquery0
        CROSS JOIN (VALUES (?), (?), (?)) subquery1
        """,
        """
        SELECT ((subquery0.column_0 * ?) + subquery1.column_0) AS res
        FROM (VALUES ROW(?), ROW(?), ROW(?)) subquery0
        CROSS JOIN (VALUES ROW(?), ROW(?), ROW(?)) subquery1
        """
      ),
      value = Seq(14, 15, 16, 24, 25, 26, 34, 35, 36),
      docs = "",
      normalize = (x: Seq[Int]) => x.sorted
    )

    test("joinValuesAndTable") - checker(
      query = Text {
        for {
          name <- db.values(Seq("Socks", "Face Mask", "Camera"))
          product <- Product.join(_.name === name)
        } yield (name, product.price)
      },
      sqls = Seq(
        """
        SELECT subquery0.column1 AS res_0, product1.price AS res_1
        FROM (VALUES (?), (?), (?)) subquery0
        JOIN product product1 ON (product1.name = subquery0.column1)
        """,
        """
        SELECT subquery0.c1 AS res_0, product1.price AS res_1
        FROM (VALUES (?), (?), (?)) subquery0
        JOIN product product1 ON (product1.name = subquery0.c1)
        """,
        """
        SELECT subquery0.column_0 AS res_0, product1.price AS res_1
        FROM (VALUES ROW(?), ROW(?), ROW(?)) subquery0
        JOIN product product1 ON (product1.name = subquery0.column_0)
        """
      ),
      value = Seq(("Socks", 3.14), ("Face Mask", 8.88), ("Camera", 1000.0)),
      docs =
        "You can also mix `values` calls and normal `selects` in the same query, e.g. with joins",
      normalize = (x: Seq[(String, Double)]) => x.sortBy(_._2)
    )

    test("multiple") {
      test("tuple") - checker(
        query = Text {
          db.values(Seq((1, 2), (3, 4), (5, 6)))
        },
        sqls = Seq("VALUES (?, ?), (?, ?), (?, ?)", "VALUES ROW(?, ?), ROW(?, ?), ROW(?, ?)"),
        value = Seq((1, 2), (3, 4), (5, 6)),
        docs = """
            `values` supports tuples and other data structures as well
        """
      )
      test("caseClass") - checker(
        query = Text {
          db.values(
            Seq(
              Buyer[Sc](1, "hello", LocalDate.parse("2001-02-03")),
              Buyer[Sc](2, "world", LocalDate.parse("2004-05-06"))
            )
          )
        },
        sqls = Seq("VALUES (?, ?, ?), (?, ?, ?)", "VALUES ROW(?, ?, ?), ROW(?, ?, ?)"),
        value = Seq(
          Buyer[Sc](1, "hello", LocalDate.parse("2001-02-03")),
          Buyer[Sc](2, "world", LocalDate.parse("2004-05-06"))
        )
      )

      test("map") - checker(
        query = Text {
          db.values(Seq((1, 2), (3, 4), (5, 6))).map { case (a, b) => (a + 10, b + 100) }
        },
        sqls = Seq(
          """
            SELECT (subquery0.column1 + ?) AS res_0, (subquery0.column2 + ?) AS res_1
            FROM (VALUES (?, ?), (?, ?), (?, ?)) subquery0
          """,
          """
            SELECT (subquery0.c1 + ?) AS res_0, (subquery0.c2 + ?) AS res_1
            FROM (VALUES (?, ?), (?, ?), (?, ?)) subquery0
          """,
          """
            SELECT (subquery0.column_0 + ?) AS res_0, (subquery0.column_1 + ?) AS res_1
            FROM (VALUES ROW(?, ?), ROW(?, ?), ROW(?, ?)) subquery0
          """
        ),
        value = Seq((11, 102), (13, 104), (15, 106)),
        docs = """
            `values` supports tuples and other data structures as well
        """
      )
      test("mapCaseClass") - checker(
        query = Text {
          val buyers = Seq(
            Buyer[Sc](1, "hello", LocalDate.parse("2001-02-03")),
            Buyer[Sc](2, "world", LocalDate.parse("2004-05-06"))
          )
          val query = db.values(buyers).map { b => (b.id + 100, b) }
          query
        },
        sqls = Seq(
          """
            SELECT
              (subquery0.column1 + ?) AS res_0,
              subquery0.column1 AS res_1_id,
              subquery0.column2 AS res_1_name,
              subquery0.column3 AS res_1_date_of_birth
            FROM (VALUES (?, ?, ?), (?, ?, ?)) subquery0
          """,
          """
            SELECT
              (subquery0.c1 + ?) AS res_0,
              subquery0.c1 AS res_1_id,
              subquery0.c2 AS res_1_name,
              subquery0.c3 AS res_1_date_of_birth
            FROM (VALUES (?, ?, ?), (?, ?, ?)) subquery0
          """,
          """
            SELECT
              (subquery0.column_0 + ?) AS res_0,
              subquery0.column_0 AS res_1_id,
              subquery0.column_1 AS res_1_name,
              subquery0.column_2 AS res_1_date_of_birth
            FROM (VALUES ROW(?, ?, ?), ROW(?, ?, ?)) subquery0
          """
        ),
        value = Seq(
          (101, Buyer[Sc](1, "hello", LocalDate.parse("2001-02-03"))),
          (102, Buyer[Sc](2, "world", LocalDate.parse("2004-05-06")))
        ),
        docs = """
            `values` supports tuples and other data structures as well
        """
      )
      test("caseClassContains") - checker(
        query = Text {
          val buyers = Seq(
            Buyer[Sc](2, "叉烧包", LocalDate.parse("1923-11-12")),
            Buyer[Sc](3, "Li Haoyi", LocalDate.parse("1965-08-09"))
          )
          Buyer.select.filter(!db.values(buyers).contains(_))
        },
        sqls = Seq(
          """
            SELECT
              buyer0.id AS id,
              buyer0.name AS name,
              buyer0.date_of_birth AS date_of_birth
            FROM buyer buyer0
            WHERE (NOT
              ((buyer0.id, buyer0.name, buyer0.date_of_birth) IN (VALUES (?, ?, ?), (?, ?, ?))))
          """,
          """
            SELECT
              buyer0.id AS id,
              buyer0.name AS name,
              buyer0.date_of_birth AS date_of_birth
            FROM buyer buyer0
            WHERE (NOT
              ((buyer0.id, buyer0.name, buyer0.date_of_birth) IN (VALUES ROW(?, ?, ?), ROW(?, ?, ?))))
          """
        ),
        value = Seq(
          Buyer[Sc](1, "James Bond", LocalDate.parse("2001-02-03"))
        ),
        docs = """
          You can use `.contains` on multi-column Scala values, which are translated
          to a SQL `IN` clause on a tuple.
        """
      )
    }
  }
}
