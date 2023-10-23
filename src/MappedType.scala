package usql

import java.time.{Instant, LocalDate, LocalDateTime, LocalTime, OffsetDateTime, OffsetTime, ZonedDateTime}

// What Quill does
// https extends//github.com/zio/zio-quill/blob/43ee1dab4f717d7e6683aa24c391740f3d17df50/quill-jdbc/src/main/scala/io/getquill/context/jdbc/Encoders.scala#L104

sealed trait MappedType[T]  {
  def castType: String
}
object MappedType{
  
  implicit object StringType extends MappedType[String] {
    def castType = "LONGVARCHAR"
  }

  implicit object ByteType extends MappedType[Byte] {
    def castType = "TINYINT"
  }

  implicit object ShortType extends MappedType[Short] {
    def castType = "SMALLINT"
  }

  implicit object IntType extends MappedType[Int] {
    def castType = "INT"
  }

  implicit object LongType extends MappedType[Long] {
    def castType = "BIGINT"
  }

  implicit object DoubleType extends MappedType[Double] {
    def castType = "DOUBLE"
  }

  implicit object BooleanType extends MappedType[Boolean] {
    def castType = "BOOLEAN"
  }

  implicit object LocalDateType extends MappedType[LocalDate] {
    def castType = "DATE"
  }

  implicit object LocalTimeType extends MappedType[LocalTime] {
    def castType = "TIME"
  }

  implicit object LocalDateTimeType extends MappedType[LocalDateTime] {
    def castType = "TIMESTAMP"
  }

  implicit object ZonedDateTimeType extends MappedType[ZonedDateTime] {
    def castType = "TIMESTAMP WITH TIMEZONE"
  }

  implicit object InstantType extends MappedType[Instant] {
    def castType = "TIMESTAMP WITH TIMEZONE"
  }

  implicit object OffsetTimeType extends MappedType[OffsetTime] {
    def castType = "TIME WITH TIMEZONE"
  }

  implicit object OffsetDateTimeType extends MappedType[OffsetDateTime] {
    def castType = "TIMESTAMP WITH TIMEZONE"
  }
}