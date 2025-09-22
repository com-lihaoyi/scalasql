package scalasql.operations

import scalasql._
import utest._
import utils.ScalaSqlSuite
import java.time.{LocalDate, LocalDateTime}
import java.util.UUID
import scala.math.BigDecimal
import sourcecode.Text

trait DbCountOpsAdvancedTests extends ScalaSqlSuite {
  def description = "Advanced COUNT operations with complex types and edge cases"

  // Advanced table with complex types for testing corner cases
  case class AdvancedData[T[_]](
    id: T[Int],
    uuid: T[UUID],
    bigDecimalValue: T[BigDecimal],
    timestamp: T[LocalDateTime],
    date: T[LocalDate],
    booleanFlag: T[Boolean],
    optionalString: T[Option[String]],
    optionalBigDecimal: T[Option[BigDecimal]],
    emptyStringField: T[String],
    zeroValue: T[Int],
    largeNumber: T[Long]
  )
  object AdvancedData extends Table[AdvancedData]

  def tests = Tests {
    test("setup") - checker(
      query = Text { AdvancedData.insert.batched(
        _.id, _.uuid, _.bigDecimalValue, _.timestamp, _.date, _.booleanFlag,
        _.optionalString, _.optionalBigDecimal, _.emptyStringField, _.zeroValue, _.largeNumber
      )(
        (1, UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), BigDecimal("999.99"),
         LocalDateTime.of(2024, 1, 1, 12, 0), LocalDate.of(2024, 1, 1), true,
         Some("test"), Some(BigDecimal("100.50")), "", 0, 999999999999L),
        (2, UUID.fromString("123e4567-e89b-12d3-a456-426614174001"), BigDecimal("0.01"),
         LocalDateTime.of(2024, 1, 2, 13, 30), LocalDate.of(2024, 1, 2), false,
         None, None, "", 0, 888888888888L),
        (3, UUID.fromString("123e4567-e89b-12d3-a456-426614174002"), BigDecimal("1000000.00"),
         LocalDateTime.of(2024, 1, 3, 14, 45), LocalDate.of(2024, 1, 3), true,
         Some(""), Some(BigDecimal("0.0001")), "not empty", 1, 777777777777L),
        (4, UUID.fromString("123e4567-e89b-12d3-a456-426614174003"), BigDecimal("0.00"),
         LocalDateTime.of(2024, 1, 4, 15, 0), LocalDate.of(2024, 1, 4), false,
         Some("duplicate"), None, "", 1, 666666666666L),
        (5, UUID.fromString("123e4567-e89b-12d3-a456-426614174004"), BigDecimal("0.00"),
         LocalDateTime.of(2024, 1, 5, 16, 15), LocalDate.of(2024, 1, 5), true,
         Some("duplicate"), Some(BigDecimal("0.00")), "not empty", 2, 555555555555L)
      ) },
      value = 5
    )

    test("countComplexTypes") - {
      test("uuidCount") - checker(
        query = Text { AdvancedData.select.countBy(_.uuid) },
        sql = "SELECT COUNT(advanced_data0.uuid) AS res FROM advanced_data advanced_data0",
        value = 5
      )

      test("uuidCountDistinct") - checker(
        query = Text { AdvancedData.select.countDistinctBy(_.uuid) },
        sql = "SELECT COUNT(DISTINCT advanced_data0.uuid) AS res FROM advanced_data advanced_data0",
        value = 5  // All UUIDs are unique
      )

      test("bigDecimalCount") - checker(
        query = Text { AdvancedData.select.countBy(_.bigDecimalValue) },
        sql = "SELECT COUNT(advanced_data0.big_decimal_value) AS res FROM advanced_data advanced_data0",
        value = 5
      )

      test("bigDecimalCountDistinct") - checker(
        query = Text { AdvancedData.select.countDistinctBy(_.bigDecimalValue) },
        sql = "SELECT COUNT(DISTINCT advanced_data0.big_decimal_value) AS res FROM advanced_data advanced_data0",
        value = 4  // Two 0.00 values are duplicates
      )

      test("dateTimeCount") - checker(
        query = Text { AdvancedData.select.countBy(_.timestamp) },
        sql = "SELECT COUNT(advanced_data0.timestamp) AS res FROM advanced_data advanced_data0",
        value = 5
      )

      test("dateCount") - checker(
        query = Text { AdvancedData.select.countDistinctBy(_.date) },
        sql = "SELECT COUNT(DISTINCT advanced_data0.date) AS res FROM advanced_data advanced_data0",
        value = 5  // All dates are unique
      )
    }

    test("countWithBooleanExpressions") - {
      test("booleanColumnCount") - checker(
        query = Text { AdvancedData.select.countBy(_.booleanFlag) },
        sql = "SELECT COUNT(advanced_data0.boolean_flag) AS res FROM advanced_data advanced_data0",
        value = 5
      )

      test("booleanColumnCountDistinct") - checker(
        query = Text { AdvancedData.select.countDistinctBy(_.booleanFlag) },
        sql = "SELECT COUNT(DISTINCT advanced_data0.boolean_flag) AS res FROM advanced_data advanced_data0",
        value = 2  // true and false
      )
    }

    test("countWithEmptyStringsAndZeros") - {
      test("emptyStringCount") - checker(
        query = Text { AdvancedData.select.countBy(_.emptyStringField) },
        sql = "SELECT COUNT(advanced_data0.empty_string_field) AS res FROM advanced_data advanced_data0",
        value = 5  // Empty strings are counted (not NULL)
      )

      test("emptyStringCountDistinct") - checker(
        query = Text { AdvancedData.select.countDistinctBy(_.emptyStringField) },
        sql = "SELECT COUNT(DISTINCT advanced_data0.empty_string_field) AS res FROM advanced_data advanced_data0",
        value = 2  // Empty string and "not empty"
      )

      test("zeroValueCount") - checker(
        query = Text { AdvancedData.select.countBy(_.zeroValue) },
        sql = "SELECT COUNT(advanced_data0.zero_value) AS res FROM advanced_data advanced_data0",
        value = 5  // Zero values are counted (not NULL)
      )

      test("zeroValueCountDistinct") - checker(
        query = Text { AdvancedData.select.countDistinctBy(_.zeroValue) },
        sql = "SELECT COUNT(DISTINCT advanced_data0.zero_value) AS res FROM advanced_data advanced_data0",
        value = 3  // 0, 1, 2
      )
    }

    test("countWithStringExpressions") - {
      test("stringConcatCount") - checker(
        query = Text { AdvancedData.select
          .filter(_.optionalString.isDefined)
          .map(a => a.optionalString.get + "_suffix")
          .count },
        sql = """SELECT COUNT(CONCAT(advanced_data0.optional_string, ?)) AS res
                FROM advanced_data advanced_data0
                WHERE (advanced_data0.optional_string IS NOT NULL)""",
        value = 4  // Non-null optional strings
      )

      test("stringLengthCount") - checker(
        query = Text { AdvancedData.select.map(_.emptyStringField.length).countDistinct },
        sql = "SELECT COUNT(DISTINCT LENGTH(advanced_data0.empty_string_field)) AS res FROM advanced_data advanced_data0",
        value = 2  // Length 0 and length 9 ("not empty")
      )
    }

    test("countWithArithmeticExpressions") - {
      test("bigDecimalArithmetic") - checker(
        query = Text { AdvancedData.select.map(a => a.bigDecimalValue * 2).countDistinct },
        sql = """SELECT COUNT(DISTINCT (advanced_data0.big_decimal_value * ?)) AS res
                FROM advanced_data advanced_data0""",
        value = 4  // Distinct doubled values
      )

      test("largeNumberModulo") - checker(
        query = Text { AdvancedData.select.map(a => a.largeNumber % 1000000).countDistinct },
        sql = """SELECT COUNT(DISTINCT (advanced_data0.large_number % ?)) AS res
                FROM advanced_data advanced_data0""",
        value = 5  // All different modulo values
      )
    }

    test("countWithComplexGroupBy") - {
      test("groupByBooleanWithCountUuid") - checker(
        query = Text { AdvancedData.select.groupBy(_.booleanFlag)(agg => agg.countBy(_.uuid)) },
        sql = """SELECT advanced_data0.boolean_flag AS res_0, COUNT(advanced_data0.uuid) AS res_1
                FROM advanced_data advanced_data0
                GROUP BY advanced_data0.boolean_flag""",
        value = Seq((false, 2), (true, 3)),
        normalize = (x: Seq[(Boolean, Int)]) => x.sortBy(_._1)
      )

      test("groupByDateWithCountBigDecimal") - checker(
        query = Text { AdvancedData.select.groupBy(_.date)(agg => agg.countBy(_.optionalBigDecimal)) },
        sql = """SELECT advanced_data0.date AS res_0, COUNT(advanced_data0.optional_big_decimal) AS res_1
                FROM advanced_data advanced_data0
                GROUP BY advanced_data0.date""",
        value = Seq(
          (LocalDate.of(2024, 1, 1), 1),
          (LocalDate.of(2024, 1, 2), 0), // NULL optional value
          (LocalDate.of(2024, 1, 3), 1),
          (LocalDate.of(2024, 1, 4), 0), // NULL optional value
          (LocalDate.of(2024, 1, 5), 1)
        ),
        normalize = (x: Seq[(LocalDate, Int)]) => x.sortBy(_._1)
      )
    }

    test("countWithFilter") - {
      test("countWithLargeNumbers") - checker(
        query = Text { AdvancedData.select
          .filter(_.largeNumber > 600000000000L)
          .countBy(_.largeNumber) },
        sql = """SELECT COUNT(advanced_data0.large_number) AS res
                FROM advanced_data advanced_data0
                WHERE (advanced_data0.large_number > ?)""",
        value = 4  // 4 records with large_number > 600000000000L
      )

      test("countWithPrecisionDecimals") - checker(
        query = Text { AdvancedData.select
          .filter(_.bigDecimalValue > BigDecimal("0.01"))
          .countDistinctBy(a => (a.bigDecimalValue * 100).floor) },
        sql = """SELECT COUNT(DISTINCT FLOOR((advanced_data0.big_decimal_value * ?))) AS res
                FROM advanced_data advanced_data0
                WHERE (advanced_data0.big_decimal_value > ?)""",
        value = 3  // Different floor values for decimal calculations
      )
    }

    test("countWithComplexPredicates") - {
      test("countWithDateRange") - checker(
        query = Text { AdvancedData.select
          .filter(a => a.date >= LocalDate.of(2024, 1, 2) && a.date <= LocalDate.of(2024, 1, 4))
          .countBy(_.timestamp) },
        sql = """SELECT COUNT(advanced_data0.timestamp) AS res
                FROM advanced_data advanced_data0
                WHERE ((advanced_data0.date >= ?) AND (advanced_data0.date <= ?))""",
        value = 3  // Records for Jan 2, 3, 4
      )
    }
  }
}