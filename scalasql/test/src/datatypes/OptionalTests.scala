package scalasql.datatypes

import scalasql.query.Expr
import scalasql.{datatypes, _}
import utest._
import utils.ScalaSqlSuite
import sourcecode.Text

case class OptCols[+T[_]](myInt: T[Option[Int]], myInt2: T[Option[Int]])

object OptCols extends Table[OptCols] {
  val metadata = initMetadata()
}

trait OptionalTests extends ScalaSqlSuite {
  def description =
    "Queries using columns that may be `NULL`, `Expr[Option[T]]` or `Option[T] in Scala"
  override def utestBeforeEach(path: Seq[String]): Unit = checker.reset()
  def tests = Tests {

    checker(
      query = Text {
        OptCols.insert.batched(_.myInt, _.myInt2)(
          (None, None),
          (Some(1), Some(2)),
          (Some(3), None),
          (None, Some(4))
        )
      },
      value = 4
    )(implicitly, utest.framework.TestPath(Nil))

    test("selectAll") - checker(
      query = Text { OptCols.select },
      sql = """
        SELECT
          opt_cols0.my_int as res__my_int,
          opt_cols0.my_int2 as res__my_int2
        FROM opt_cols opt_cols0
      """,
      value = Seq(
        OptCols[Id](None, None),
        OptCols[Id](Some(1), Some(2)),
        OptCols[Id](Some(3), None),
        OptCols[Id](None, Some(4))
      )
    )

    test("groupByMaxGet") - checker(
      query = Text { OptCols.select.groupBy(_.myInt)(_.maxByOpt(_.myInt2.get)) },
      sql = """
        SELECT opt_cols0.my_int as res__0, MAX(opt_cols0.my_int2) as res__1
        FROM opt_cols opt_cols0
        GROUP BY opt_cols0.my_int
      """,
      value = Seq(None -> Some(4), Some(1) -> Some(2), Some(3) -> None),
      normalize = (x: Seq[(Option[Int], Option[Int])]) => x.sorted
    )

    test("isDefined") - checker(
      query = Text { OptCols.select.filter(_.myInt.isDefined) },
      sql = """
        SELECT
          opt_cols0.my_int as res__my_int,
          opt_cols0.my_int2 as res__my_int2
        FROM opt_cols opt_cols0
        WHERE opt_cols0.my_int IS NOT NULL""",
      value = Seq(OptCols[Id](Some(1), Some(2)), OptCols[Id](Some(3), None))
    )

    test("isEmpty") - checker(
      query = Text { OptCols.select.filter(_.myInt.isEmpty) },
      sql = """
        SELECT
          opt_cols0.my_int as res__my_int,
          opt_cols0.my_int2 as res__my_int2
        FROM opt_cols opt_cols0
        WHERE opt_cols0.my_int IS NULL""",
      value = Seq(OptCols[Id](None, None), OptCols[Id](None, Some(4)))
    )

    test("sqlEquals") {
      test("nonOptionHit") - checker(
        query = Text { OptCols.select.filter(_.myInt `=` 1) },
        sql = """
          SELECT
            opt_cols0.my_int as res__my_int,
            opt_cols0.my_int2 as res__my_int2
          FROM opt_cols opt_cols0
          WHERE opt_cols0.my_int = ?
        """,
        value = Seq(OptCols[Id](Some(1), Some(2)))
      )

      test("nonOptionMiss") - checker(
        query = Text { OptCols.select.filter(_.myInt `=` 2) },
        sql = """
          SELECT
            opt_cols0.my_int as res__my_int,
            opt_cols0.my_int2 as res__my_int2
          FROM opt_cols opt_cols0
          WHERE opt_cols0.my_int = ?
        """,
        value = Seq[OptCols[Id]]()
      )

      test("optionMiss") - checker( // SQL null = null is false
        query = Text { OptCols.select.filter(_.myInt `=` Option.empty[Int]) },
        sql = """
          SELECT
            opt_cols0.my_int as res__my_int,
            opt_cols0.my_int2 as res__my_int2
          FROM opt_cols opt_cols0
          WHERE opt_cols0.my_int = ?
        """,
        value = Seq[OptCols[Id]]()
      )
    }
    test("scalaEquals") {
      test("someHit") - checker(
        query = Text { OptCols.select.filter(_.myInt === Option(1)) },
        sqls = Seq(
          """
            SELECT
              opt_cols0.my_int as res__my_int,
              opt_cols0.my_int2 as res__my_int2
            FROM opt_cols opt_cols0
            WHERE opt_cols0.my_int IS NOT DISTINCT FROM ?
          """,
          // MySQL syntax
          """
            SELECT
              opt_cols0.my_int as res__my_int,
              opt_cols0.my_int2 as res__my_int2
            FROM opt_cols opt_cols0
            WHERE opt_cols0.my_int <=> ?
          """
        ),
        value = Seq(OptCols[Id](Some(1), Some(2)))
      )

      test("noneHit") - checker(
        query = Text { OptCols.select.filter(_.myInt === Option.empty[Int]) },
        sqls = Seq(
          """
            SELECT
              opt_cols0.my_int as res__my_int,
              opt_cols0.my_int2 as res__my_int2
            FROM opt_cols opt_cols0
            WHERE opt_cols0.my_int IS NOT DISTINCT FROM ?
          """,
          // MySQL syntax
          """
            SELECT
              opt_cols0.my_int as res__my_int,
              opt_cols0.my_int2 as res__my_int2
            FROM opt_cols opt_cols0
            WHERE opt_cols0.my_int <=> ?
          """
        ),
        value = Seq(OptCols[Id](None, None), OptCols[Id](None, Some(4)))
      )

      test("notEqualsSome") - checker(
        query = Text {
          OptCols.select.filter(_.myInt !== Option(1))
        },
        sqls = Seq(
          """
          SELECT
            opt_cols0.my_int as res__my_int,
            opt_cols0.my_int2 as res__my_int2
          FROM opt_cols opt_cols0
          WHERE opt_cols0.my_int IS DISTINCT FROM ?
          """,
          // MySQL syntax
          """
          SELECT
            opt_cols0.my_int as res__my_int,
            opt_cols0.my_int2 as res__my_int2
          FROM opt_cols opt_cols0
          WHERE (NOT opt_cols0.my_int <=> ?)
          """
        ),
        value = Seq(
          OptCols[Id](None, None),
          OptCols[Id](Some(3), None),
          OptCols[Id](None, Some(value = 4))
        )
      )

      test("notEqualsNone") - checker(
        query = Text {
          OptCols.select.filter(_.myInt !== Option.empty[Int])
        },
        sqls = Seq(
          """
          SELECT
            opt_cols0.my_int as res__my_int,
            opt_cols0.my_int2 as res__my_int2
          FROM opt_cols opt_cols0
          WHERE opt_cols0.my_int IS DISTINCT FROM ?
          """,
          // MySQL syntax
          """
            SELECT
              opt_cols0.my_int as res__my_int,
              opt_cols0.my_int2 as res__my_int2
            FROM opt_cols opt_cols0
            WHERE (NOT opt_cols0.my_int <=> ?)
          """
        ),
        value = Seq(
          OptCols[Id](Some(1), Some(2)),
          OptCols[Id](Some(3), None)
        )
      )

    }

    test("map") - checker(
      query = Text { OptCols.select.map(d => d.copy[Expr](myInt = d.myInt.map(_ + 10))) },
      sql = """
      SELECT
        opt_cols0.my_int + ? as res__my_int,
        opt_cols0.my_int2 as res__my_int2
      FROM opt_cols opt_cols0
      """,
      value = Seq(
        OptCols[Id](None, None),
        OptCols[Id](Some(11), Some(2)),
        OptCols[Id](Some(13), None),
        OptCols[Id](None, Some(4))
      )
    )

    test("map2") - checker(
      query = Text { OptCols.select.map(_.myInt.map(_ + 10)) },
      sql = "SELECT opt_cols0.my_int + ? as res FROM opt_cols opt_cols0",
      value = Seq(None, Some(11), Some(13), None)
    )

    test("flatMap") - checker(
      query = Text {
        OptCols.select
          .map(d => d.copy[Expr](myInt = d.myInt.flatMap(v => d.myInt2.map(v2 => v + v2 + 10))))
      },
      sql = """
        SELECT
          opt_cols0.my_int + opt_cols0.my_int2 + ? as res__my_int,
          opt_cols0.my_int2 as res__my_int2
        FROM opt_cols opt_cols0
      """,
      value = Seq(
        OptCols[Id](None, None),
        OptCols[Id](Some(13), Some(2)),
        // because my_int2 is added to my_int, and my_int2 is null, my_int becomes null too
        OptCols[Id](None, None),
        OptCols[Id](None, Some(4))
      )
    )

    test("mapGet") - checker(
      query = Text {
        OptCols.select.map(d => d.copy[Expr](myInt = d.myInt.map(_ + d.myInt2.get + 1)))
      },
      sql = """
        SELECT
          opt_cols0.my_int + opt_cols0.my_int2 + ? as res__my_int,
          opt_cols0.my_int2 as res__my_int2
        FROM opt_cols opt_cols0
      """,
      value = Seq(
        OptCols[Id](None, None),
        OptCols[Id](Some(4), Some(2)),
        // because my_int2 is added to my_int, and my_int2 is null, my_int becomes null too
        OptCols[Id](None, None),
        OptCols[Id](None, Some(4))
      )
    )

    test("rawGet") - checker(
      query = Text {
        OptCols.select.map(d => d.copy[Expr](myInt = d.myInt.get + d.myInt2.get + 1))
      },
      sql = """
        SELECT
          opt_cols0.my_int + opt_cols0.my_int2 + ? as res__my_int,
          opt_cols0.my_int2 as res__my_int2
        FROM opt_cols opt_cols0
      """,
      value = Seq(
        OptCols[Id](None, None),
        OptCols[Id](Some(4), Some(2)),
        // because my_int2 is added to my_int, and my_int2 is null, my_int becomes null too
        OptCols[Id](None, None),
        OptCols[Id](None, Some(4))
      )
    )

    test("getOrElse") - checker(
      query = Text { OptCols.select.map(d => d.copy[Expr](myInt = d.myInt.getOrElse(-1))) },
      sql = """
        SELECT
          COALESCE(opt_cols0.my_int, ?) as res__my_int,
          opt_cols0.my_int2 as res__my_int2
        FROM opt_cols opt_cols0
      """,
      value = Seq(
        OptCols[Id](Some(-1), None),
        OptCols[Id](Some(1), Some(2)),
        OptCols[Id](Some(3), None),
        OptCols[Id](Some(-1), Some(4))
      )
    )

    test("orElse") - checker(
      query = Text { OptCols.select.map(d => d.copy[Expr](myInt = d.myInt.orElse(d.myInt2))) },
      sql = """
        SELECT
          COALESCE(opt_cols0.my_int, opt_cols0.my_int2) as res__my_int,
          opt_cols0.my_int2 as res__my_int2
        FROM opt_cols opt_cols0
      """,
      value = Seq(
        OptCols[Id](None, None),
        OptCols[Id](Some(1), Some(2)),
        OptCols[Id](Some(3), None),
        OptCols[Id](Some(4), Some(4))
      )
    )

    test("filter") - checker(
      query = Text { OptCols.select.map(d => d.copy[Expr](myInt = d.myInt.filter(_ < 2))) },
      sql = """
        SELECT
          CASE
            WHEN opt_cols0.my_int < ? THEN opt_cols0.my_int
            ELSE NULL
          END as res__my_int,
          opt_cols0.my_int2 as res__my_int2
        FROM opt_cols opt_cols0
      """,
      value = Seq(
        OptCols[Id](None, None),
        OptCols[Id](Some(1), Some(2)),
        OptCols[Id](None, None),
        OptCols[Id](None, Some(4))
      )
    )
    test("sorting") {
      test("nullsLast") - checker(
        query = Text { OptCols.select.sortBy(_.myInt).nullsLast },
        sqls = Seq(
          """
            SELECT opt_cols0.my_int as res__my_int, opt_cols0.my_int2 as res__my_int2
            FROM opt_cols opt_cols0
            ORDER BY res__my_int NULLS LAST
          """,
          """
            SELECT opt_cols0.my_int as res__my_int, opt_cols0.my_int2 as res__my_int2
            FROM opt_cols opt_cols0
            ORDER BY res__my_int IS NULL ASC, res__my_int
          """
        ),
        value = Seq(
          OptCols[Id](Some(1), Some(2)),
          OptCols[Id](Some(3), None),
          OptCols[Id](None, None),
          OptCols[Id](None, Some(4))
        )
      )
      test("nullsFirst") - checker(
        query = Text { OptCols.select.sortBy(_.myInt).nullsFirst },
        sqls = Seq(
          """
            SELECT opt_cols0.my_int as res__my_int, opt_cols0.my_int2 as res__my_int2
            FROM opt_cols opt_cols0
            ORDER BY res__my_int NULLS FIRST
          """,
          """
            SELECT opt_cols0.my_int as res__my_int, opt_cols0.my_int2 as res__my_int2
            FROM opt_cols opt_cols0
            ORDER BY res__my_int IS NULL DESC, res__my_int
          """
        ),
        value = Seq(
          OptCols[Id](None, None),
          OptCols[Id](None, Some(4)),
          OptCols[Id](Some(1), Some(2)),
          OptCols[Id](Some(3), None)
        )
      )
      test("ascNullsLast") - checker(
        query = Text { OptCols.select.sortBy(_.myInt).asc.nullsLast },
        sqls = Seq(
          """
            SELECT opt_cols0.my_int as res__my_int, opt_cols0.my_int2 as res__my_int2
            FROM opt_cols opt_cols0
            ORDER BY res__my_int ASC NULLS LAST
          """,
          """
            SELECT opt_cols0.my_int as res__my_int, opt_cols0.my_int2 as res__my_int2
            FROM opt_cols opt_cols0
            ORDER BY res__my_int IS NULL ASC, res__my_int ASC
          """
        ),
        value = Seq(
          OptCols[Id](Some(1), Some(2)),
          OptCols[Id](Some(3), None),
          OptCols[Id](None, None),
          OptCols[Id](None, Some(4))
        )
      )
      test("ascNullsFirst") - checker(
        query = Text { OptCols.select.sortBy(_.myInt).asc.nullsFirst },
        sqls = Seq(
          """
            SELECT opt_cols0.my_int as res__my_int, opt_cols0.my_int2 as res__my_int2
            FROM opt_cols opt_cols0
            ORDER BY res__my_int ASC NULLS FIRST
          """,
          """
            SELECT opt_cols0.my_int as res__my_int, opt_cols0.my_int2 as res__my_int2
            FROM opt_cols opt_cols0
            ORDER BY res__my_int ASC
          """
        ),
        value = Seq(
          OptCols[Id](None, None),
          OptCols[Id](None, Some(4)),
          OptCols[Id](Some(1), Some(2)),
          OptCols[Id](Some(3), None)
        )
      )
      test("descNullsLast") - checker(
        query = Text { OptCols.select.sortBy(_.myInt).desc.nullsLast },
        sqls = Seq(
          """
            SELECT opt_cols0.my_int as res__my_int, opt_cols0.my_int2 as res__my_int2
            FROM opt_cols opt_cols0
            ORDER BY res__my_int DESC NULLS LAST
          """,
          """
            SELECT opt_cols0.my_int as res__my_int, opt_cols0.my_int2 as res__my_int2
            FROM opt_cols opt_cols0
            ORDER BY res__my_int DESC
          """
        ),
        value = Seq(
          OptCols[Id](Some(3), None),
          OptCols[Id](Some(1), Some(2)),
          OptCols[Id](None, None),
          OptCols[Id](None, Some(4))
        )
      )
      test("descNullsFirst") - checker(
        query = Text { OptCols.select.sortBy(_.myInt).desc.nullsFirst },
        sqls = Seq(
          """
            SELECT opt_cols0.my_int as res__my_int, opt_cols0.my_int2 as res__my_int2
            FROM opt_cols opt_cols0
            ORDER BY res__my_int DESC NULLS FIRST
          """,
          """
            SELECT opt_cols0.my_int as res__my_int, opt_cols0.my_int2 as res__my_int2
            FROM opt_cols opt_cols0
            ORDER BY res__my_int IS NULL DESC, res__my_int DESC
          """
        ),
        value = Seq(
          OptCols[Id](None, None),
          OptCols[Id](None, Some(4)),
          OptCols[Id](Some(3), None),
          OptCols[Id](Some(1), Some(2))
        )
      )
    }
  }
}
