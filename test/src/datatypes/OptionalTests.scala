package scalasql.datatypes

import scalasql.query.Expr
import scalasql.{datatypes, _}
import utest._
import utils.ScalaSqlSuite

case class DataTypesOpt[+T[_]](
    myInt: T[Option[Int]],
)

object DataTypesOpt extends Table[DataTypesOpt] {
  val metadata = initMetadata
}

/**
 * Tests for basic query operations: map, filter, join, etc.
 */
trait OptionalTests extends ScalaSqlSuite {
  def tests = Tests {

    checker(
      query = DataTypesOpt.insert.batched(_.myInt)(
        None,
        Some(1),
      ),
      value = 2
    )

    test("selectAll") - checker(
      query = DataTypesOpt.select,
      value = Seq(
        DataTypesOpt[Id](None),
        DataTypesOpt[Id](Some(1))
      )
    )

    test("isDefined") - checker(
      query = DataTypesOpt.select.filter(_.myInt.isDefined),
      value = Seq(
        DataTypesOpt[Id](Some(1))
      )
    )

    test("isEmpty") - checker(
      query = DataTypesOpt.select.filter(_.myInt.isEmpty),
      value = Seq(
        DataTypesOpt[Id](None)
      )
    )

    test("sqlEqualsNonOptionHit") - checker(
      query = DataTypesOpt.select.filter(_.myInt `=` 1),
      value = Seq(
        DataTypesOpt[Id](Some(1))
      )
    )

    test("sqlEqualsNonOptionMiss") - checker(
      query = DataTypesOpt.select.filter(_.myInt `=` 2),
      value = Seq[DataTypesOpt[Id]]()
    )

    test("sqlEqualsOptionMiss") - checker( // SQL null = null is false
      query = DataTypesOpt.select.filter(_.myInt `=` Option.empty[Int]),
      value = Seq[DataTypesOpt[Id]]()
    )


    test("scalaEqualsSomeHit") - checker(
      query = DataTypesOpt.select.filter(_.myInt === Option(1)),
      value = Seq(
        DataTypesOpt[Id](Some(1))
      )
    )

    test("scalaEqualsNoneHit") - checker(
      query = DataTypesOpt.select.filter(_.myInt === Option.empty[Int]),
      value = Seq(
        DataTypesOpt[Id](None)
      )
    )

    test("map") - checker(
      query = DataTypesOpt
        .select
        .map(d => d.copy[Expr](myInt = d.myInt.map(_ + 10))),
      value = Seq(
        DataTypesOpt[Id](None),
        DataTypesOpt[Id](Some(11))
      )
    )

    test("map2") - checker(
      query = DataTypesOpt
        .select
        .map(_.myInt.map(_ + 10)),
      value = Seq(
        None,
        Some(11)
      )
    )
  }
}
