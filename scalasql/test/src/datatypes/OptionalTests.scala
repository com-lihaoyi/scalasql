package scalasql.datatypes

import scalasql.core.Expr
import scalasql._
import utest._
import utils.ScalaSqlSuite
import sourcecode.Text

import java.time.{
  Instant,
  LocalDate,
  LocalDateTime,
  LocalTime,
  OffsetDateTime,
  ZoneId,
  ZonedDateTime
}
import java.util.Date
import java.text.SimpleDateFormat
import java.util.UUID

case class OptCols[T[_]](myInt: T[Option[Int]], myInt2: T[Option[Int]])

object OptCols extends Table[OptCols]

trait OptionalTests extends ScalaSqlSuite {
  def description =
    "Queries using columns that may be `NULL`, `Expr[Option[T]]` or `Option[T]` in Scala"
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
          opt_cols0.my_int AS my_int,
          opt_cols0.my_int2 AS my_int2
        FROM opt_cols opt_cols0
      """,
      value = Seq(
        OptCols[Sc](None, None),
        OptCols[Sc](Some(1), Some(2)),
        OptCols[Sc](Some(3), None),
        OptCols[Sc](None, Some(4))
      ),
      docs = """
        Nullable columns are modelled as `T[Option[V]]` fields on your `case class`,
        and are returned to you as `Option[V]` values when you run a query. These
        can be `Some` or `None`
      """
    )

    test("groupByMaxGet") - checker(
      query = Text { OptCols.select.groupBy(_.myInt)(_.maxByOpt(_.myInt2.get)) },
      sql = """
        SELECT opt_cols0.my_int AS res_0, MAX(opt_cols0.my_int2) AS res_1
        FROM opt_cols opt_cols0
        GROUP BY opt_cols0.my_int
      """,
      value = Seq(None -> Some(4), Some(1) -> Some(2), Some(3) -> None),
      normalize = (x: Seq[(Option[Int], Option[Int])]) => x.sorted,
      docs = """
        Some aggregates return `Expr[Option[V]]`s, et.c. `.maxByOpt`
      """
    )

    test("isDefined") - checker(
      query = Text { OptCols.select.filter(_.myInt.isDefined) },
      sql = """
        SELECT
          opt_cols0.my_int AS my_int,
          opt_cols0.my_int2 AS my_int2
        FROM opt_cols opt_cols0
        WHERE (opt_cols0.my_int IS NOT NULL)""",
      value = Seq(OptCols[Sc](Some(1), Some(2)), OptCols[Sc](Some(3), None)),
      docs = """
        `.isDefined` on `Expr[Option[V]]` translates to a SQL
        `IS NOT NULL` check
      """
    )

    test("isEmpty") - checker(
      query = Text { OptCols.select.filter(_.myInt.isEmpty) },
      sql = """
        SELECT
          opt_cols0.my_int AS my_int,
          opt_cols0.my_int2 AS my_int2
        FROM opt_cols opt_cols0
        WHERE (opt_cols0.my_int IS NULL)""",
      value = Seq(OptCols[Sc](None, None), OptCols[Sc](None, Some(4))),
      docs = """
        `.isEmpty` on `Expr[Option[V]]` translates to a SQL
        `IS NULL` check
      """
    )

    test("sqlEquals") {
      test("nonOptionHit") - checker(
        query = Text { OptCols.select.filter(_.myInt `=` 1) },
        sql = """
          SELECT
            opt_cols0.my_int AS my_int,
            opt_cols0.my_int2 AS my_int2
          FROM opt_cols opt_cols0
          WHERE (opt_cols0.my_int = ?)
        """,
        value = Seq(OptCols[Sc](Some(1), Some(2))),
        docs = """
          Backticked `=` equality in ScalaSQL translates to a raw `=`
          in SQL. This follows SQL `NULL` semantics, meaning that
          `None = None` returns `false` rather than `true`
        """
      )

      test("nonOptionMiss") - checker(
        query = Text { OptCols.select.filter(_.myInt `=` 2) },
        sql = """
          SELECT
            opt_cols0.my_int AS my_int,
            opt_cols0.my_int2 AS my_int2
          FROM opt_cols opt_cols0
          WHERE (opt_cols0.my_int = ?)
        """,
        value = Seq[OptCols[Sc]]()
      )

      test("optionMiss") - checker( // SQL null = null is false
        query = Text { OptCols.select.filter(_.myInt `=` Option.empty[Int]) },
        sql = """
          SELECT
            opt_cols0.my_int AS my_int,
            opt_cols0.my_int2 AS my_int2
          FROM opt_cols opt_cols0
          WHERE (opt_cols0.my_int = ?)
        """,
        value = Seq[OptCols[Sc]]()
      )
    }
    test("scalaEquals") {
      test("someHit") - checker(
        query = Text { OptCols.select.filter(_.myInt === Option(1)) },
        sqls = Seq(
          """
            SELECT
              opt_cols0.my_int AS my_int,
              opt_cols0.my_int2 AS my_int2
            FROM opt_cols opt_cols0
            WHERE (opt_cols0.my_int IS NOT DISTINCT FROM ?)
          """,
          // MySQL syntax
          """
            SELECT
              opt_cols0.my_int AS my_int,
              opt_cols0.my_int2 AS my_int2
            FROM opt_cols opt_cols0
            WHERE (opt_cols0.my_int <=> ?)
          """
        ),
        value = Seq(OptCols[Sc](Some(1), Some(2))),
        docs = """
          `===` equality in ScalaSQL translates to a `IS NOT DISTINCT` in SQL.
          This roughly follows Scala `==` semantics, meaning `None === None`
          returns `true`
        """
      )

      test("noneHit") - checker(
        query = Text { OptCols.select.filter(_.myInt === Option.empty[Int]) },
        sqls = Seq(
          """
            SELECT
              opt_cols0.my_int AS my_int,
              opt_cols0.my_int2 AS my_int2
            FROM opt_cols opt_cols0
            WHERE (opt_cols0.my_int IS NOT DISTINCT FROM ?)
          """,
          // MySQL syntax
          """
            SELECT
              opt_cols0.my_int AS my_int,
              opt_cols0.my_int2 AS my_int2
            FROM opt_cols opt_cols0
            WHERE (opt_cols0.my_int <=> ?)
          """
        ),
        value = Seq(OptCols[Sc](None, None), OptCols[Sc](None, Some(4)))
      )

      test("notEqualsSome") - checker(
        query = Text {
          OptCols.select.filter(_.myInt !== Option(1))
        },
        sqls = Seq(
          """
          SELECT
            opt_cols0.my_int AS my_int,
            opt_cols0.my_int2 AS my_int2
          FROM opt_cols opt_cols0
          WHERE (opt_cols0.my_int IS DISTINCT FROM ?)
          """,
          // MySQL syntax
          """
          SELECT
            opt_cols0.my_int AS my_int,
            opt_cols0.my_int2 AS my_int2
          FROM opt_cols opt_cols0
          WHERE (NOT (opt_cols0.my_int <=> ?))
          """
        ),
        value = Seq(
          OptCols[Sc](None, None),
          OptCols[Sc](Some(3), None),
          OptCols[Sc](None, Some(value = 4))
        )
      )

      test("notEqualsNone") - checker(
        query = Text {
          OptCols.select.filter(_.myInt !== Option.empty[Int])
        },
        sqls = Seq(
          """
          SELECT
            opt_cols0.my_int AS my_int,
            opt_cols0.my_int2 AS my_int2
          FROM opt_cols opt_cols0
          WHERE (opt_cols0.my_int IS DISTINCT FROM ?)
          """,
          // MySQL syntax
          """
            SELECT
              opt_cols0.my_int AS my_int,
              opt_cols0.my_int2 AS my_int2
            FROM opt_cols opt_cols0
            WHERE (NOT (opt_cols0.my_int <=> ?))
          """
        ),
        value = Seq(
          OptCols[Sc](Some(1), Some(2)),
          OptCols[Sc](Some(3), None)
        )
      )

    }

    test("map") - checker(
      query = Text { OptCols.select.map(d => d.copy[Expr](myInt = d.myInt.map(_ + 10))) },
      sql = """
      SELECT
        (opt_cols0.my_int + ?) AS my_int,
        opt_cols0.my_int2 AS my_int2
      FROM opt_cols opt_cols0
      """,
      value = Seq(
        OptCols[Sc](None, None),
        OptCols[Sc](Some(11), Some(2)),
        OptCols[Sc](Some(13), None),
        OptCols[Sc](None, Some(4))
      ),
      docs = """
        You can use operators like `.map` and `.flatMap` to work with
        your `Expr[Option[V]]` values. These roughly follow the semantics
        that you would be familiar with from Scala.
      """
    )

    test("map2") - checker(
      query = Text { OptCols.select.map(_.myInt.map(_ + 10)) },
      sql = "SELECT (opt_cols0.my_int + ?) AS res FROM opt_cols opt_cols0",
      value = Seq(None, Some(11), Some(13), None)
    )

    test("flatMap") - checker(
      query = Text {
        OptCols.select
          .map(d => d.copy[Expr](myInt = d.myInt.flatMap(v => d.myInt2.map(v2 => v + v2 + 10))))
      },
      sql = """
        SELECT
          ((opt_cols0.my_int + opt_cols0.my_int2) + ?) AS my_int,
          opt_cols0.my_int2 AS my_int2
        FROM opt_cols opt_cols0
      """,
      value = Seq(
        OptCols[Sc](None, None),
        OptCols[Sc](Some(13), Some(2)),
        // because my_int2 is added to my_int, and my_int2 is null, my_int becomes null too
        OptCols[Sc](None, None),
        OptCols[Sc](None, Some(4))
      )
    )

    test("mapGet") - checker(
      query = Text {
        OptCols.select.map(d => d.copy[Expr](myInt = d.myInt.map(_ + d.myInt2.get + 1)))
      },
      sql = """
        SELECT
          ((opt_cols0.my_int + opt_cols0.my_int2) + ?) AS my_int,
          opt_cols0.my_int2 AS my_int2
        FROM opt_cols opt_cols0
      """,
      value = Seq(
        OptCols[Sc](None, None),
        OptCols[Sc](Some(4), Some(2)),
        // because my_int2 is added to my_int, and my_int2 is null, my_int becomes null too
        OptCols[Sc](None, None),
        OptCols[Sc](None, Some(4))
      ),
      docs = """
        You can use `.get` to turn an `Expr[Option[V]]` into an `Expr[V]`. This follows
        SQL semantics, such that `NULL`s anywhere in that selected column automatically
        will turn the whole column `None` (if it's an `Expr[Option[V]]` column) or `null`
        (if it's not an optional column)
      """
    )

    test("rawGet") - checker(
      query = Text {
        OptCols.select.map(d => d.copy[Expr](myInt = d.myInt.get + d.myInt2.get + 1))
      },
      sql = """
        SELECT
          ((opt_cols0.my_int + opt_cols0.my_int2) + ?) AS my_int,
          opt_cols0.my_int2 AS my_int2
        FROM opt_cols opt_cols0
      """,
      value = Seq(
        OptCols[Sc](None, None),
        OptCols[Sc](Some(4), Some(2)),
        // because my_int2 is added to my_int, and my_int2 is null, my_int becomes null too
        OptCols[Sc](None, None),
        OptCols[Sc](None, Some(4))
      )
    )

    test("getOrElse") - checker(
      query = Text { OptCols.select.map(d => d.copy[Expr](myInt = d.myInt.getOrElse(-1))) },
      sql = """
        SELECT
          COALESCE(opt_cols0.my_int, ?) AS my_int,
          opt_cols0.my_int2 AS my_int2
        FROM opt_cols opt_cols0
      """,
      value = Seq(
        OptCols[Sc](Some(-1), None),
        OptCols[Sc](Some(1), Some(2)),
        OptCols[Sc](Some(3), None),
        OptCols[Sc](Some(-1), Some(4))
      )
    )

    test("orElse") - checker(
      query = Text { OptCols.select.map(d => d.copy[Expr](myInt = d.myInt.orElse(d.myInt2))) },
      sql = """
        SELECT
          COALESCE(opt_cols0.my_int, opt_cols0.my_int2) AS my_int,
          opt_cols0.my_int2 AS my_int2
        FROM opt_cols opt_cols0
      """,
      value = Seq(
        OptCols[Sc](None, None),
        OptCols[Sc](Some(1), Some(2)),
        OptCols[Sc](Some(3), None),
        OptCols[Sc](Some(4), Some(4))
      )
    )

    test("filter") - checker(
      query = Text { OptCols.select.map(d => d.copy[Expr](myInt = d.myInt.filter(_ < 2))) },
      sql = """
        SELECT
          CASE
            WHEN (opt_cols0.my_int < ?) THEN opt_cols0.my_int
            ELSE NULL
          END AS my_int,
          opt_cols0.my_int2 AS my_int2
        FROM opt_cols opt_cols0
      """,
      value = Seq(
        OptCols[Sc](None, None),
        OptCols[Sc](Some(1), Some(2)),
        OptCols[Sc](None, None),
        OptCols[Sc](None, Some(4))
      ),
      docs = """
        `.filter` follows normal Scala semantics, and translates to a `CASE`/`WHEN (foo)`/`ELSE NULL`
      """
    )
    test("sorting") {
      test("nullsLast") - checker(
        query = Text { OptCols.select.sortBy(_.myInt).nullsLast },
        sqls = Seq(
          """
            SELECT opt_cols0.my_int AS my_int, opt_cols0.my_int2 AS my_int2
            FROM opt_cols opt_cols0
            ORDER BY my_int NULLS LAST
          """,
          """
            SELECT opt_cols0.my_int AS my_int, opt_cols0.my_int2 AS my_int2
            FROM opt_cols opt_cols0
            ORDER BY my_int IS NULL ASC, my_int
          """,
          """
            SELECT opt_cols0.my_int AS my_int, opt_cols0.my_int2 AS my_int2
            FROM opt_cols opt_cols0
            ORDER BY IIF(my_int IS NULL, 1, 0), my_int
          """
        ),
        value = Seq(
          OptCols[Sc](Some(1), Some(2)),
          OptCols[Sc](Some(3), None),
          OptCols[Sc](None, None),
          OptCols[Sc](None, Some(4))
        ),
        moreValues = Seq(
          Seq(
            OptCols[Sc](Some(1), Some(2)),
            OptCols[Sc](Some(3), None),
            OptCols[Sc](None, Some(4)),
            OptCols[Sc](None, None)
          )
        ),
        docs = """
          `.nullsLast` and `.nullsFirst` translate to SQL `NULLS LAST` and `NULLS FIRST` clauses
        """
      )
      test("nullsFirst") - checker(
        query = Text { OptCols.select.sortBy(_.myInt).nullsFirst },
        sqls = Seq(
          """
            SELECT opt_cols0.my_int AS my_int, opt_cols0.my_int2 AS my_int2
            FROM opt_cols opt_cols0
            ORDER BY my_int NULLS FIRST
          """,
          """
            SELECT opt_cols0.my_int AS my_int, opt_cols0.my_int2 AS my_int2
            FROM opt_cols opt_cols0
            ORDER BY my_int IS NULL DESC, my_int
          """,
          """
            SELECT opt_cols0.my_int AS my_int, opt_cols0.my_int2 AS my_int2
            FROM opt_cols opt_cols0
            ORDER BY IIF(my_int IS NULL, 0, 1), my_int
          """
        ),
        value = Seq(
          OptCols[Sc](None, None),
          OptCols[Sc](None, Some(4)),
          OptCols[Sc](Some(1), Some(2)),
          OptCols[Sc](Some(3), None)
        ),
        moreValues = Seq(
          Seq(
            OptCols[Sc](None, Some(4)),
            OptCols[Sc](None, None),
            OptCols[Sc](Some(1), Some(2)),
            OptCols[Sc](Some(3), None)
          )
        )
      )
      test("ascNullsLast") - checker(
        query = Text { OptCols.select.sortBy(_.myInt).asc.nullsLast },
        sqls = Seq(
          """
            SELECT opt_cols0.my_int AS my_int, opt_cols0.my_int2 AS my_int2
            FROM opt_cols opt_cols0
            ORDER BY my_int ASC NULLS LAST
          """,
          """
            SELECT opt_cols0.my_int AS my_int, opt_cols0.my_int2 AS my_int2
            FROM opt_cols opt_cols0
            ORDER BY my_int IS NULL ASC, my_int ASC
          """,
          """
            SELECT opt_cols0.my_int AS my_int, opt_cols0.my_int2 AS my_int2
            FROM opt_cols opt_cols0
            ORDER BY IIF(my_int IS NULL, 1, 0), my_int ASC
          """
        ),
        value = Seq(
          OptCols[Sc](Some(1), Some(2)),
          OptCols[Sc](Some(3), None),
          OptCols[Sc](None, None),
          OptCols[Sc](None, Some(4))
        ),
        moreValues = Seq(
          Seq(
            OptCols[Sc](Some(1), Some(2)),
            OptCols[Sc](Some(3), None),
            OptCols[Sc](None, Some(4)),
            OptCols[Sc](None, None)
          )
        )
      )
      test("ascNullsFirst") - checker(
        query = Text { OptCols.select.sortBy(_.myInt).asc.nullsFirst },
        sqls = Seq(
          """
            SELECT opt_cols0.my_int AS my_int, opt_cols0.my_int2 AS my_int2
            FROM opt_cols opt_cols0
            ORDER BY my_int ASC NULLS FIRST
          """,
          """
            SELECT opt_cols0.my_int AS my_int, opt_cols0.my_int2 AS my_int2
            FROM opt_cols opt_cols0
            ORDER BY my_int ASC
          """
        ),
        value = Seq(
          OptCols[Sc](None, None),
          OptCols[Sc](None, Some(4)),
          OptCols[Sc](Some(1), Some(2)),
          OptCols[Sc](Some(3), None)
        ),
        moreValues = Seq(
          Seq(
            OptCols[Sc](None, None),
            OptCols[Sc](None, Some(4)),
            OptCols[Sc](Some(1), Some(2)),
            OptCols[Sc](Some(3), None)
          )
        )
      )
      test("descNullsLast") - checker(
        query = Text { OptCols.select.sortBy(_.myInt).desc.nullsLast },
        sqls = Seq(
          """
            SELECT opt_cols0.my_int AS my_int, opt_cols0.my_int2 AS my_int2
            FROM opt_cols opt_cols0
            ORDER BY my_int DESC NULLS LAST
          """,
          """
            SELECT opt_cols0.my_int AS my_int, opt_cols0.my_int2 AS my_int2
            FROM opt_cols opt_cols0
            ORDER BY my_int DESC
          """
        ),
        value = Seq(
          OptCols[Sc](Some(3), None),
          OptCols[Sc](Some(1), Some(2)),
          OptCols[Sc](None, None),
          OptCols[Sc](None, Some(4))
        ),
        moreValues = Seq(
          Seq(
            OptCols[Sc](Some(3), None),
            OptCols[Sc](Some(1), Some(2)),
            OptCols[Sc](None, None),
            OptCols[Sc](None, Some(4))
          )
        )
      )
      test("descNullsFirst") - checker(
        query = Text { OptCols.select.sortBy(_.myInt).desc.nullsFirst },
        sqls = Seq(
          """
            SELECT opt_cols0.my_int AS my_int, opt_cols0.my_int2 AS my_int2
            FROM opt_cols opt_cols0
            ORDER BY my_int DESC NULLS FIRST
          """,
          """
            SELECT opt_cols0.my_int AS my_int, opt_cols0.my_int2 AS my_int2
            FROM opt_cols opt_cols0
            ORDER BY my_int IS NULL DESC, my_int DESC
          """,
          """
            SELECT opt_cols0.my_int AS my_int, opt_cols0.my_int2 AS my_int2
            FROM opt_cols opt_cols0
            ORDER BY IIF(my_int IS NULL, 0, 1), my_int DESC
          """
        ),
        value = Seq(
          OptCols[Sc](None, None),
          OptCols[Sc](None, Some(4)),
          OptCols[Sc](Some(3), None),
          OptCols[Sc](Some(1), Some(2))
        ),
        moreValues = Seq(
          Seq(
            OptCols[Sc](None, Some(4)),
            OptCols[Sc](None, None),
            OptCols[Sc](Some(3), None),
            OptCols[Sc](Some(1), Some(2))
          )
        )
      )
      test("roundTripOptionalValues") - checker.recorded(
        """
        This example demonstrates a range of different data types being written
        as options, both with Some(v) and None values
        """,
        Text {
          object MyEnum extends Enumeration {
            val foo, bar, baz = Value

            implicit def make: String => Value = withName
          }
          case class OptDataTypes[T[_]](
              myTinyInt: T[Option[Byte]],
              mySmallInt: T[Option[Short]],
              myInt: T[Option[Int]],
              myBigInt: T[Option[Long]],
              myDouble: T[Option[Double]],
              myBoolean: T[Option[Boolean]],
              myLocalDate: T[Option[LocalDate]],
              myLocalTime: T[Option[LocalTime]],
              myLocalDateTime: T[Option[LocalDateTime]],
              myUtilDate: T[Option[Date]],
              myInstant: T[Option[Instant]],
              myVarBinary: T[Option[geny.Bytes]],
              myUUID: T[Option[java.util.UUID]],
              myEnum: T[Option[MyEnum.Value]]
          )

          object OptDataTypes extends Table[OptDataTypes] {
            override def tableName: String = "data_types"
          }

          val rowSome = OptDataTypes[Sc](
            myTinyInt = Some(123.toByte),
            mySmallInt = Some(12345.toShort),
            myInt = Some(12345678),
            myBigInt = Some(12345678901L),
            myDouble = Some(3.14),
            myBoolean = Some(true),
            myLocalDate = Some(LocalDate.parse("2023-12-20")),
            myLocalTime = Some(LocalTime.parse("10:15:30")),
            myLocalDateTime = Some(LocalDateTime.parse("2011-12-03T10:15:30")),
            myUtilDate = Some(
              new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").parse("2011-12-03T10:15:30.000")
            ),
            myInstant = Some(Instant.parse("2011-12-03T10:15:30Z")),
            myVarBinary = Some(new geny.Bytes(Array[Byte](1, 2, 3, 4, 5, 6, 7, 8))),
            myUUID = Some(new java.util.UUID(1234567890L, 9876543210L)),
            myEnum = Some(MyEnum.bar)
          )

          val rowNone = OptDataTypes[Sc](
            myTinyInt = None,
            mySmallInt = None,
            myInt = None,
            myBigInt = None,
            myDouble = None,
            myBoolean = None,
            myLocalDate = None,
            myLocalTime = None,
            myLocalDateTime = None,
            myUtilDate = None,
            myInstant = None,
            myVarBinary = None,
            myUUID = None,
            myEnum = None
          )

          db.run(
            OptDataTypes.insert.values(rowSome, rowNone)
          ) ==> 2

          db.run(OptDataTypes.select) ==> Seq(rowSome, rowNone)
        }
      )

    }
  }
}
