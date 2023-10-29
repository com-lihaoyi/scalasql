package scalasql.datatypes

import scalasql.query.Expr
import scalasql.{datatypes, _}
import utest._
import utils.ScalaSqlSuite

case class OptCols[+T[_]](myInt: T[Option[Int]], myInt2: T[Option[Int]])

object OptCols extends Table[OptCols] {
  val metadata = initMetadata
}

/**
 * Tests for basic query operations: map, filter, join, etc.
 */
trait OptionalTests extends ScalaSqlSuite {
  override def utestBeforeEach(path: Seq[String]): Unit = checker.reset()
  def tests = Tests {

    checker(
      query = OptCols.insert.batched(_.myInt, _.myInt2)((None, None), (Some(1), Some(2))),
      value = 2
    )

    test("selectAll") - checker(
      query = OptCols.select,
      sql = """
        SELECT
          opt_cols0.my_int as res__my_int,
          opt_cols0.my_int2 as res__my_int2
        FROM opt_cols opt_cols0
      """,
      value = Seq(OptCols[Id](None, None), OptCols[Id](Some(1), Some(2)))
    )

    test("groupByMaxGet") - checker(
      query = OptCols.select.groupBy(_.myInt)(_.maxBy(_.myInt2.get)),
      sql = """
        SELECT opt_cols0.my_int as res__0, MAX(opt_cols0.my_int2) as res__1
        FROM opt_cols opt_cols0
        GROUP BY opt_cols0.my_int
      """,
      value = Seq(
        None -> 0,
        Some(1) -> 2
      )
    )

    test("isDefined") - checker(
      query = OptCols.select.filter(_.myInt.isDefined),
      sql = """
        SELECT
          opt_cols0.my_int as res__my_int,
          opt_cols0.my_int2 as res__my_int2
        FROM opt_cols opt_cols0
        WHERE opt_cols0.my_int IS NOT NULL""",
      value = Seq(OptCols[Id](Some(1), Some(2)))
    )

    test("isEmpty") - checker(
      query = OptCols.select.filter(_.myInt.isEmpty),
      sql = """
        SELECT
          opt_cols0.my_int as res__my_int,
          opt_cols0.my_int2 as res__my_int2
        FROM opt_cols opt_cols0
        WHERE opt_cols0.my_int IS NULL""",
      value = Seq(OptCols[Id](None, None))
    )

    test("sqlEqualsNonOptionHit") - checker(
      query = OptCols.select.filter(_.myInt `=` 1),
      sql = """
        SELECT
          opt_cols0.my_int as res__my_int,
          opt_cols0.my_int2 as res__my_int2
        FROM opt_cols opt_cols0
        WHERE opt_cols0.my_int = ?
      """,
      value = Seq(OptCols[Id](Some(1), Some(2)))
    )

    test("sqlEqualsNonOptionMiss") - checker(
      query = OptCols.select.filter(_.myInt `=` 2),
      sql = """
        SELECT
          opt_cols0.my_int as res__my_int,
          opt_cols0.my_int2 as res__my_int2
        FROM opt_cols opt_cols0
        WHERE opt_cols0.my_int = ?
      """,
      value = Seq[OptCols[Id]]()
    )

    test("sqlEqualsOptionMiss") - checker( // SQL null = null is false
      query = OptCols.select.filter(_.myInt `=` Option.empty[Int]),
      sql = """
        SELECT
          opt_cols0.my_int as res__my_int,
          opt_cols0.my_int2 as res__my_int2
        FROM opt_cols opt_cols0
        WHERE opt_cols0.my_int = ?
      """,
      value = Seq[OptCols[Id]]()
    )

    test("scalaEqualsSomeHit") - checker(
      query = OptCols.select.filter(_.myInt === Option(1)),
      sql = """
        SELECT
          opt_cols0.my_int as res__my_int,
          opt_cols0.my_int2 as res__my_int2
        FROM opt_cols opt_cols0
        WHERE (opt_cols0.my_int IS NULL AND ? IS NULL) OR opt_cols0.my_int = ?
      """,
      value = Seq(OptCols[Id](Some(1), Some(2)))
    )

    test("scalaEqualsNoneHit") - checker(
      query = OptCols.select.filter(_.myInt === Option.empty[Int]),
      sql = """
        SELECT
          opt_cols0.my_int as res__my_int,
          opt_cols0.my_int2 as res__my_int2
        FROM opt_cols opt_cols0
        WHERE (opt_cols0.my_int IS NULL AND ? IS NULL) OR opt_cols0.my_int = ?
      """,
      value = Seq(OptCols[Id](None, None))
    )

    test("map") - checker(
      query = OptCols.select.map(d => d.copy[Expr](myInt = d.myInt.map(_ + 10))),
      sql = """
      SELECT
        opt_cols0.my_int + ? as res__my_int,
        opt_cols0.my_int2 as res__my_int2
      FROM opt_cols opt_cols0
      """,
      value = Seq(OptCols[Id](None, None), OptCols[Id](Some(11), Some(2)))
    )

    test("map2") - checker(
      query = OptCols.select.map(_.myInt.map(_ + 10)),
      sql = "SELECT opt_cols0.my_int + ? as res FROM opt_cols opt_cols0",
      value = Seq(None, Some(11))
    )

    test("flatMap") - checker(
      query = OptCols.select
        .map(d => d.copy[Expr](myInt = d.myInt.flatMap(v => d.myInt2.map(v2 => v + v2 + 10)))),
      sql = """
        SELECT
          opt_cols0.my_int + opt_cols0.my_int2 + ? as res__my_int,
          opt_cols0.my_int2 as res__my_int2
        FROM opt_cols opt_cols0
      """,
      value = Seq(OptCols[Id](None, None), OptCols[Id](Some(13), Some(2)))
    )

  }
}
