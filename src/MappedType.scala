package usql

import java.sql.JDBCType
import java.time.{Instant, LocalDate, LocalDateTime, LocalTime, OffsetDateTime, OffsetTime, ZonedDateTime}

// What Quill does
// https extends//github.com/zio/zio-quill/blob/43ee1dab4f717d7e6683aa24c391740f3d17df50/quill-jdbc/src/main/scala/io/getquill/context/jdbc/Encoders.scala#L104

// What SLICK does
// https://github.com/slick/slick/blob/88b2ffb177776fd74dee38124b8c54d616d1a9ae/slick/src/main/scala/slick/jdbc/JdbcTypesComponent.scala#L15
sealed trait MappedType[T]  {
  def jdbcType: JDBCType
}
object MappedType{
  implicit object StringType extends MappedType[String] {
    def jdbcType = JDBCType.LONGVARCHAR
  }

  implicit object ByteType extends MappedType[Byte] {
    def jdbcType = JDBCType.TINYINT
  }

  implicit object ShortType extends MappedType[Short] {
    def jdbcType = JDBCType.SMALLINT
  }

  implicit object IntType extends MappedType[Int] {
    def jdbcType = JDBCType.INTEGER
  }

  implicit object LongType extends MappedType[Long] {
    def jdbcType = JDBCType.BIGINT
  }

  implicit object DoubleType extends MappedType[Double] {
    def jdbcType = JDBCType.DOUBLE
  }

  implicit object BooleanType extends MappedType[Boolean] {
    def jdbcType = JDBCType.BOOLEAN
  }

  implicit object LocalDateType extends MappedType[LocalDate] {
    def jdbcType = JDBCType.DATE
  }

  implicit object LocalTimeType extends MappedType[LocalTime] {
    def jdbcType = JDBCType.TIME
  }

  implicit object LocalDateTimeType extends MappedType[LocalDateTime] {
    def jdbcType = JDBCType.TIMESTAMP
  }

  implicit object ZonedDateTimeType extends MappedType[ZonedDateTime] {
    def jdbcType = JDBCType.TIMESTAMP_WITH_TIMEZONE
  }

  implicit object InstantType extends MappedType[Instant] {
    def jdbcType = JDBCType.TIMESTAMP_WITH_TIMEZONE
  }

  implicit object OffsetTimeType extends MappedType[OffsetTime] {
    def jdbcType = JDBCType.TIME_WITH_TIMEZONE
  }

  implicit object OffsetDateTimeType extends MappedType[OffsetDateTime] {
    def jdbcType = JDBCType.TIMESTAMP_WITH_TIMEZONE
  }
}
