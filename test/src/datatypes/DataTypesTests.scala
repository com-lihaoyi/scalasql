package scalasql.datatypes

import scalasql._
import utest._

import java.time.{
  Instant,
  LocalDate,
  LocalDateTime,
  LocalTime,
  OffsetDateTime,
  OffsetTime,
  ZonedDateTime
}

/**
 * Tests for basic query operations: map, filter, join, etc.
 */
trait DataTypesTests extends ScalaSqlSuite {
  def tests = Tests {
    test("constant") {
      val value = DataTypes[Val](
        myTinyInt = 123.toByte,
        mySmallInt = 12345.toShort,
        myInt = 12345678,
        myBigInt = 12345678901L,
        myDouble = 3.14,
        myBoolean = true,
        myLocalDate = LocalDate.parse("2023-12-20"),
        myLocalTime = LocalTime.parse("10:15:30"),
        myLocalDateTime = LocalDateTime.parse("2011-12-03T10:15:30"),
        myZonedDateTime = ZonedDateTime.parse("2011-12-03T10:15:30+01:00[Europe/Paris]"),
//        myInstant = Instant.parse("2011-12-03T10:15:30Z"),
//        myOffsetDateTime = OffsetDateTime.parse("2011-12-03T10:15:30+01:00"),
      )
      checker(
        query = DataTypes.insert.values(
          _.myTinyInt -> value.myTinyInt(),
          _.mySmallInt -> value.mySmallInt(),
          _.myInt -> value.myInt(),
          _.myBigInt -> value.myBigInt(),
          _.myDouble -> value.myDouble(),
          _.myBoolean -> value.myBoolean(),
          _.myLocalDate -> value.myLocalDate(),
          _.myLocalTime -> value.myLocalTime(),
          _.myLocalDateTime -> value.myLocalDateTime(),
          _.myZonedDateTime -> value.myZonedDateTime(),
//          _.myInstant -> value.myInstant(),
//          _.myOffsetDateTime -> value.myOffsetDateTime(),
        ),
        value = 1
      )

      checker(
        query = DataTypes.select,
        value = Seq(value)
      )
    }
  }
}
