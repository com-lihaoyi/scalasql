package scalasql.datatypes

import scalasql._
import sourcecode.Text
import utest._
import utils.ScalaSqlSuite

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

import _root_.test.scalasql.WorldSqlTests.ArrowAssert

case class Nested[T[_]](
    fooId: T[Int],
    myBoolean: T[Boolean]
)
object Nested extends Table[Nested]

case class Enclosing[T[_]](
    barId: T[Int],
    myString: T[String],
    foo: Nested[T]
)
object Enclosing extends Table[Enclosing]

trait DataTypesTests extends ScalaSqlSuite {
  def description =
    "Basic operations on all the data types that ScalaSql supports " +
      "mapping between Database types and Scala types"
  def tests = Tests {
    test("constant") - checker.recorded(
      """
      This example demonstrates a range of different data types being written
      and read back via ScalaSQL
      """,
      Text {
        object MyEnum extends Enumeration {
          val foo, bar, baz = Value

          implicit def make: String => Value = withName
        }
        case class DataTypes[T[_]](
            myTinyInt: T[Byte],
            mySmallInt: T[Short],
            myInt: T[Int],
            myBigInt: T[Long],
            myDouble: T[Double],
            myBoolean: T[Boolean],
            myLocalDate: T[LocalDate],
            myLocalTime: T[LocalTime],
            myLocalDateTime: T[LocalDateTime],
            myUtilDate: T[Date],
            myInstant: T[Instant],
            myVarBinary: T[geny.Bytes],
            myUUID: T[java.util.UUID],
            myEnum: T[MyEnum.Value]
        )

        object DataTypes extends Table[DataTypes]

        val value = DataTypes[Sc](
          myTinyInt = 123.toByte,
          mySmallInt = 12345.toShort,
          myInt = 12345678,
          myBigInt = 12345678901L,
          myDouble = 3.14,
          myBoolean = true,
          myLocalDate = LocalDate.parse("2023-12-20"),
          myLocalTime = LocalTime.parse("10:15:30"),
          myLocalDateTime = LocalDateTime.parse("2011-12-03T10:15:30"),
          myUtilDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").parse("2011-12-03T10:15:30.000"),
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
            _.myUtilDate := value.myUtilDate,
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

        case class NonRoundTripTypes[T[_]](
            myZonedDateTime: T[ZonedDateTime],
            myOffsetDateTime: T[OffsetDateTime]
        )

        object NonRoundTripTypes extends Table[NonRoundTripTypes]

        val value = NonRoundTripTypes[Sc](
          myZonedDateTime = ZonedDateTime.parse("2011-12-03T10:15:30+01:00[Europe/Paris]"),
          myOffsetDateTime = OffsetDateTime.parse("2011-12-03T10:15:30+00:00")
        )

        def normalize(v: NonRoundTripTypes[Sc]) = v.copy[Sc](
          myZonedDateTime = v.myZonedDateTime.withZoneSameInstant(ZoneId.systemDefault),
          myOffsetDateTime = v.myOffsetDateTime.withOffsetSameInstant(OffsetDateTime.now.getOffset)
        )

        db.run(
          NonRoundTripTypes.insert.columns(
            _.myOffsetDateTime := value.myOffsetDateTime,
            _.myZonedDateTime := value.myZonedDateTime
          )
        ) ==> 1

        db.run(NonRoundTripTypes.select).map(normalize) ==> Seq(normalize(value))
      }
    )

    test("enclosing") - checker.recorded(
      """
      You can nest `case class`es in other `case class`es to DRY up common sets of
      table columns. These nested `case class`es have their columns flattened out
      into the enclosing `case class`'s columns, such that at the SQL level it is
      all flattened out without nesting.
      """,
      Text {
        // case class Nested[T[_]](
        //   fooId: T[Int],
        //   myBoolean: T[Boolean],
        // )
        // object Nested extends Table[Nested]
        //
        // case class Enclosing[T[_]](
        //     barId: T[Int],
        //     myString: T[String],
        //     foo: Nested[T]
        // )
        // object Enclosing extends Table[Enclosing]
        val value1 = Enclosing[Sc](
          barId = 1337,
          myString = "hello",
          foo = Nested[Sc](
            fooId = 271828,
            myBoolean = true
          )
        )
        val value2 = Enclosing[Sc](
          barId = 31337,
          myString = "world",
          foo = Nested[Sc](
            fooId = 1618,
            myBoolean = false
          )
        )

        val insertColumns = Enclosing.insert.columns(
          _.barId := value1.barId,
          _.myString := value1.myString,
          _.foo.fooId := value1.foo.fooId,
          _.foo.myBoolean := value1.foo.myBoolean
        )
        db.renderSql(insertColumns) ==>
          "INSERT INTO enclosing (bar_id, my_string, foo_id, my_boolean) VALUES (?, ?, ?, ?)"

        db.run(insertColumns) ==> 1

        val insertValues = Enclosing.insert.values(value2)
        db.renderSql(insertValues) ==>
          "INSERT INTO enclosing (bar_id, my_string, foo_id, my_boolean) VALUES (?, ?, ?, ?)"

        db.run(insertValues) ==> 1

        db.renderSql(Enclosing.select) ==> """
          SELECT
            enclosing0.bar_id AS bar_id,
            enclosing0.my_string AS my_string,
            enclosing0.foo_id AS foo_id,
            enclosing0.my_boolean AS my_boolean
          FROM enclosing enclosing0
        """

        db.run(Enclosing.select) ==> Seq(value1, value2)

      }
    )
  }
}
