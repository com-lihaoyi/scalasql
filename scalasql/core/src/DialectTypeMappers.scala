package scalasql.core

import java.util.UUID
import java.time.{
  LocalDate,
  LocalTime,
  LocalDateTime,
  ZonedDateTime,
  Instant,
  OffsetTime,
  OffsetDateTime
}

/**
 * A default set of data type mappers that need to be present in any ScalaSql dialect
 */
trait DialectTypeMappers extends DialectConfig {
  implicit val dialectSelf: DialectTypeMappers

  implicit def StringType: TypeMapper[String]
  implicit def ByteType: TypeMapper[Byte]
  implicit def ShortType: TypeMapper[Short]
  implicit def IntType: TypeMapper[Int]
  implicit def LongType: TypeMapper[Long]

  implicit def FloatType: TypeMapper[Float]
  implicit def DoubleType: TypeMapper[Double]
  implicit def BigDecimalType: TypeMapper[scala.math.BigDecimal]
  implicit def BooleanType: TypeMapper[Boolean]
  implicit def UuidType: TypeMapper[UUID]
  implicit def BytesType: TypeMapper[geny.Bytes]
  implicit def UtilDateType: TypeMapper[java.util.Date]
  implicit def LocalDateType: TypeMapper[LocalDate]
  implicit def LocalTimeType: TypeMapper[LocalTime]

  implicit def LocalDateTimeType: TypeMapper[LocalDateTime]

  implicit def ZonedDateTimeType: TypeMapper[ZonedDateTime]
  implicit def InstantType: TypeMapper[Instant]

  implicit def OffsetTimeType: TypeMapper[OffsetTime]

  implicit def OffsetDateTimeType: TypeMapper[OffsetDateTime]
  implicit def EnumType[T <: Enumeration#Value](implicit constructor: String => T): TypeMapper[T]
  implicit def OptionType[T](implicit inner: TypeMapper[T]): TypeMapper[Option[T]]
}
