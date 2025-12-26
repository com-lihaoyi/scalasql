package scalasql.simple.datatypes

import scalasql.simple.{*, given}
import scalasql.utils.ScalaSqlSuite

import sourcecode.Text
import utest._

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

case class Nested(
    fooId: Int,
    myBoolean: Boolean
) extends SimpleTable.Nested
object Nested extends SimpleTable[Nested]

case class Enclosing(
    barId: Int,
    myString: String,
    foo: Nested
)
object Enclosing extends SimpleTable[Enclosing]

trait SimpleTableDataTypesTests extends ScalaSqlSuite {
  def description =
    "Basic operations on all the data types that ScalaSql supports " +
      "mapping between Database types and Scala types."
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
        case class DataTypes(
            myTinyInt: Byte,
            mySmallInt: Short,
            myInt: Int,
            myBigInt: Long,
            myDouble: Double,
            myBoolean: Boolean,
            myLocalDate: LocalDate,
            myLocalTime: LocalTime,
            myLocalDateTime: LocalDateTime,
            myUtilDate: Date,
            myInstant: Instant,
            myVarBinary: geny.Bytes,
            myUUID: java.util.UUID,
            myEnum: MyEnum.Value
        )

        object DataTypes extends SimpleTable[DataTypes]

        val value = DataTypes(
          myTinyInt = 123.toByte,
          mySmallInt = 12345.toShort,
          myInt = 12345678,
          myBigInt = 12345678901L,
          myDouble = 3.14,
          myBoolean = true,
          myLocalDate = LocalDate.parse("2023-12-20"),
          myLocalTime = LocalTime.parse("10:15:30"),
          myLocalDateTime = LocalDateTime.parse("2011-12-03T10:15:30"),
          myUtilDate =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").parse("2011-12-03T10:15:30.000"),
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

        case class NonRoundTripTypes(
            myZonedDateTime: ZonedDateTime,
            myOffsetDateTime: OffsetDateTime
        )

        object NonRoundTripTypes extends SimpleTable[NonRoundTripTypes]

        val value = NonRoundTripTypes(
          myZonedDateTime = ZonedDateTime.parse("2011-12-03T10:15:30+01:00[Europe/Paris]"),
          myOffsetDateTime = OffsetDateTime.parse("2011-12-03T10:15:30+00:00")
        )

        def normalize(v: NonRoundTripTypes) = v.copy(
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

    // !! Important: '- with SimpleTable' so it will be detected by generateDocs.mill
    test("enclosing - with SimpleTable") - checker.recorded(
      """
      You can nest `case class`es in other `case class`es to DRY up common sets of
      table columns. These nested `case class`es have their columns flattened out
      into the enclosing `case class`'s columns, such that at the SQL level it is
      all flattened out without nesting.

      **Important**: When using nested `case class`es with `SimpleTable`,
      make sure to extend `SimpleTable.Nested` in the nested class.
      """,
      Text {
        // case class Nested(
        //   fooId: Int,
        //   myBoolean: Boolean,
        // ) extends SimpleTable.Nested
        // object Nested extends SimpleTable[Nested]
        //
        // case class Enclosing(
        //     barId: Int,
        //     myString: String,
        //     foo: Nested
        // )
        // object Enclosing extends SimpleTable[Enclosing]
        val value1 = Enclosing(
          barId = 1337,
          myString = "hello",
          foo = Nested(
            fooId = 271828,
            myBoolean = true
          )
        )
        val value2 = Enclosing(
          barId = 31337,
          myString = "world",
          foo = Nested(
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
    test("JoinNullable proper type mapping") - checker.recorded(
      "",
      Text {
        case class A(id: Int, bId: Option[Int])
        object A extends SimpleTable[A]

        object Custom extends Enumeration {
          val Foo, Bar = Value

          implicit def make: String => Value = withName
        }

        case class B(id: Int, custom: Custom.Value)
        object B extends SimpleTable[B]
        db.run(A.insert.columns(_.id := 1, _.bId := None))
        val result = db.run(A.select.leftJoin(B)(_.id === _.id).single)
        result._2 ==> None
      }
    )
  }
}
