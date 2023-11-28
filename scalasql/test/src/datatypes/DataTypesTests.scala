package scalasql.datatypes

import scalasql.{datatypes, _}
import utest._
import utils.ScalaSqlSuite

import java.time.{
  Instant,
  LocalDate,
  LocalDateTime,
  LocalTime,
  OffsetDateTime,
  OffsetTime,
  ZoneId,
  ZoneOffset,
  ZonedDateTime
}

case class DataTypes[+T[_]](
    myTinyInt: T[Byte],
    mySmallInt: T[Short],
    myInt: T[Int],
    myBigInt: T[Long],
    myDouble: T[Double],
    myBoolean: T[Boolean],
    myLocalDate: T[LocalDate],
    myLocalTime: T[LocalTime],
    myLocalDateTime: T[LocalDateTime],
    myInstant: T[Instant],
    myVarBinary: T[geny.Bytes],
    myUUID: T[java.util.UUID]
)

object DataTypes extends Table[DataTypes] {
  initTableMetadata()
}

case class NonRoundTripTypes[+T[_]](
    myZonedDateTime: T[ZonedDateTime],
    myOffsetDateTime: T[OffsetDateTime]
)

object NonRoundTripTypes extends Table[NonRoundTripTypes] {
  initTableMetadata()
}

trait DataTypesTests extends ScalaSqlSuite {
  def description =
    "Basic operations on all the data types that ScalaSql supports " +
      "mapping between Database types and Scala types"
  def tests = Tests {
    test("constant") {
      val value = DataTypes[Id](
        myTinyInt = 123.toByte,
        mySmallInt = 12345.toShort,
        myInt = 12345678,
        myBigInt = 12345678901L,
        myDouble = 3.14,
        myBoolean = true,
        myLocalDate = LocalDate.parse("2023-12-20"),
        myLocalTime = LocalTime.parse("10:15:30"),
        myLocalDateTime = LocalDateTime.parse("2011-12-03T10:15:30"),
        myInstant = Instant.parse("2011-12-03T10:15:30Z"),
        myVarBinary = new geny.Bytes(Array[Byte](1, 2, 3, 4, 5, 6, 7, 8)),
        myUUID = new java.util.UUID(1234567890L, 9876543210L)
      )
      checker(
        query = DataTypes.insert.columns(
          _.myTinyInt := value.myTinyInt,
          _.mySmallInt := value.mySmallInt,
          _.myInt := value.myInt,
          _.myBigInt := value.myBigInt,
          _.myDouble := value.myDouble,
          _.myBoolean := value.myBoolean,
          _.myLocalDate := value.myLocalDate,
          _.myLocalTime := value.myLocalTime,
          _.myLocalDateTime := value.myLocalDateTime,
          _.myInstant := value.myInstant,
          _.myVarBinary := value.myVarBinary,
          _.myUUID := value.myUUID
        ),
        value = 1
      )

      checker(query = DataTypes.select, value = Seq(value))
    }

    // In general, databases do not store timezones and offsets together with their timestamps:
    // "TIMESTAMP WITH TIMEZONE" is a lie and it actually stores UTC and renders to whatever
    // timezone the client queries it from. Thus values of type `OffsetDateTime` can preserve
    // their instant, but cannot be round-tripped preserving the offset.
    test("nonRoundTrip") {
      val value = NonRoundTripTypes[Id](
        myZonedDateTime = ZonedDateTime.parse("2011-12-03T10:15:30+01:00[Europe/Paris]"),
        myOffsetDateTime = OffsetDateTime.parse("2011-12-03T10:15:30+00:00")
      )

      def normalize(v: NonRoundTripTypes[Id]) = v.copy[Id](
        myZonedDateTime = v.myZonedDateTime.withZoneSameInstant(ZoneId.systemDefault),
        myOffsetDateTime = v.myOffsetDateTime.withOffsetSameInstant(OffsetDateTime.now.getOffset)
      )

      checker(
        query = NonRoundTripTypes.insert.columns(
          _.myOffsetDateTime := value.myOffsetDateTime,
          _.myZonedDateTime := value.myZonedDateTime
        ),
        value = 1
      )

      checker(
        query = NonRoundTripTypes.select,
        value = Seq(normalize(value)),
        normalize = (x: Seq[datatypes.NonRoundTripTypes[Id]]) => x.map(normalize)
      )
    }
  }
}
