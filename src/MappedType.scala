package scalasql

import java.sql.JDBCType
import java.time.{Instant, LocalDate, LocalDateTime, LocalTime, OffsetDateTime, OffsetTime, ZoneOffset, ZonedDateTime}

// What Quill does
// https extends//github.com/zio/zio-quill/blob/43ee1dab4f717d7e6683aa24c391740f3d17df50/quill-jdbc/src/main/scala/io/getquill/context/jdbc/Encoders.scala#L104

// What SLICK does
// https://github.com/slick/slick/blob/88b2ffb177776fd74dee38124b8c54d616d1a9ae/slick/src/main/scala/slick/jdbc/JdbcTypesComponent.scala#L15

// Official JDBC mapping docs
// https://docs.oracle.com/javase/tutorial/jdbc/basics/index.html
// https://docs.oracle.com/javase/1.5.0/docs/guide/jdbc/getstart/mapping.html#1055162
sealed trait MappedType[T] {
  def jdbcType: JDBCType
  def fromObject: PartialFunction[Object, T]
}
object MappedType {
  implicit object StringType extends MappedType[String] {
    def jdbcType = JDBCType.LONGVARCHAR
    def fromObject = { case t: String => t }
  }

  implicit object ByteType extends MappedType[Byte] {
    def jdbcType = JDBCType.TINYINT
    def fromObject = {
      case t: java.lang.Number => t.byteValue()
    }
  }

  implicit object ShortType extends MappedType[Short] {
    def jdbcType = JDBCType.SMALLINT
    def fromObject = {
      case t: java.lang.Number => t.shortValue()
    }
  }

  implicit object IntType extends MappedType[Int] {
    def jdbcType = JDBCType.INTEGER
    def fromObject = {
      case t: java.lang.Number => t.intValue()
    }
  }

  implicit object LongType extends MappedType[Long] {
    def jdbcType = JDBCType.BIGINT
    def fromObject = {
      case t: java.lang.Number => t.longValue()
    }
  }

  implicit object DoubleType extends MappedType[Double] {
    def jdbcType = JDBCType.DOUBLE
    def fromObject = {
      case t: java.lang.Number => t.doubleValue()
    }
  }

  implicit object BooleanType extends MappedType[Boolean] {
    def jdbcType = JDBCType.BOOLEAN
    def fromObject = {
      case t: java.lang.Boolean => t
      case t: java.lang.Number if t.intValue() == 0 => false
      case t: java.lang.Number if t.intValue() == 1 => true
    }
  }

  implicit object LocalDateType extends MappedType[LocalDate] {
    def jdbcType = JDBCType.DATE
    def fromObject = {
      case t: java.sql.Date => t.toLocalDate
      case s: String => LocalDate.parse(s)
    }
  }

  implicit object LocalTimeType extends MappedType[LocalTime] {
    def jdbcType = JDBCType.TIME
    def fromObject = {
      case t: java.sql.Time => t.toLocalTime
      case s: String => LocalTime.parse(s)
    }
  }

  implicit object LocalDateTimeType extends MappedType[LocalDateTime] {
    def jdbcType = JDBCType.TIMESTAMP
    def fromObject = {
      case t: java.sql.Timestamp => t.toLocalDateTime
      case s: String => LocalDateTime.parse(s)
    }
  }

  implicit object ZonedDateTimeType extends MappedType[ZonedDateTime] {
    def jdbcType = JDBCType.TIMESTAMP_WITH_TIMEZONE
    def fromObject = {
      case t: java.time.OffsetDateTime => t.toZonedDateTime
      case s: String => ZonedDateTime.parse(s)
    }
  }

  implicit object InstantType extends MappedType[Instant] {
    def jdbcType = JDBCType.TIMESTAMP_WITH_TIMEZONE
    def fromObject = {
      case t: java.time.OffsetDateTime => t.toInstant
      case s: String => Instant.parse(s)
    }
  }

  implicit object OffsetTimeType extends MappedType[OffsetTime] {
    def jdbcType = JDBCType.TIME_WITH_TIMEZONE

    def fromObject = {
      case t: java.time.OffsetTime => t
      case s: String => OffsetTime.parse(s)
    }
  }

  implicit object OffsetDateTimeType extends MappedType[OffsetDateTime] {
    def jdbcType = JDBCType.TIMESTAMP_WITH_TIMEZONE
    def fromObject = {
      case t: java.time.OffsetDateTime => t
      case t: java.sql.Timestamp => t.toInstant.atOffset(ZoneOffset.UTC)
      case t: java.time.LocalDateTime => t.atOffset(ZoneOffset.UTC)
      case s: String => OffsetDateTime.parse(s)
    }
  }
}
