package scalasql.datatypes

import scalasql.{datatypes, _}
import sourcecode.Text
import utest._
import utils.ScalaSqlSuite

import java.sql.{JDBCType, PreparedStatement, ResultSet}
import java.time.{Instant, LocalDate, LocalDateTime, LocalTime, OffsetDateTime, OffsetTime, ZoneId, ZoneOffset, ZonedDateTime}

object MyEnum extends Enumeration {
  val foo, bar, baz = Value

  implicit def make: String => Value = withName
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
    myUUID: T[java.util.UUID],
    myEnum: T[MyEnum.Value]
)

object DataTypes extends Table[DataTypes]

case class NonRoundTripTypes[+T[_]](
    myZonedDateTime: T[ZonedDateTime],
    myOffsetDateTime: T[OffsetDateTime]
)

object NonRoundTripTypes extends Table[NonRoundTripTypes]


case class Nested[+T[_]](
  fooId: T[Int],
  myBoolean: T[Boolean],
)
object Nested extends Table[Nested]

case class Extending[+T[_]](
    barId: T[Int],
    myString: T[String],
    foo: Nested[T]
)
object Extending extends Table[Extending]


trait DataTypesTests extends ScalaSqlSuite {
  def description =
    "Basic operations on all the data types that ScalaSql supports " +
      "mapping between Database types and Scala types"
  def tests = Tests {
    test("constant")  - checker.recorded(
      """
      This example demonstrates a range of different data types being written
      and read back via ScalaSQL
      """,
      Text {
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
          myUUID = new java.util.UUID(1234567890L, 9876543210L),
          myEnum = MyEnum.bar
        )

        db.run(
          DataTypes.insert.columns(
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
            _.myUUID := value.myUUID,
            _.myEnum := value.myEnum
          )
        ) ==> 1

        db.run(DataTypes.select) ==> Seq(value)
      }
    )

    test("nonRoundTrip") - checker.recorded(
      """
      In general, databases do not store timezones and offsets together with their timestamps:
      "TIMESTAMP WITH TIMEZONE" is a lie and it actually stores UTC and renders to whatever
      timezone the client queries it from. Thus values of type `OffsetDateTime` can preserve
      their instant, but cannot be round-tripped preserving the offset.
      """,
      Text {
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
    )

    test("extending") - checker.recorded (
      """

      """,
      Text {
        val value1 = Extending[Id](
          barId = 1337,
          myString = "hello",
          foo = Nested[Id](
            fooId = 271828,
            myBoolean = true
          )
        )
        val value2 = Extending[Id](
          barId = 31337,
          myString = "world",
          foo = Nested[Id](
            fooId = 1618,
            myBoolean = false
          )
        )

        checker(
          query = Extending.insert.columns(
            _.barId := value1.barId,
            _.myString := value1.myString,
            _.foo.fooId := value1.foo.fooId,
            _.foo.myBoolean := value1.foo.myBoolean,
          ),
          value = 1
        )

        checker(
          query = Extending.insert.values(value2),
          value = 1
        )

        checker(
          query = Extending.select,
          value = Seq(value1, value2)
        )
      }
    )
  }
}
