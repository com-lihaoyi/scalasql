package scalasql

import java.sql.{JDBCType, PreparedStatement, ResultSet}
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

// What Quill does
// https://github.com/zio/zio-quill/blob/43ee1dab4f717d7e6683aa24c391740f3d17df50/quill-jdbc/src/main/scala/io/getquill/context/jdbc/Encoders.scala#L104

// What SLICK does
// https://github.com/slick/slick/blob/88b2ffb177776fd74dee38124b8c54d616d1a9ae/slick/src/main/scala/slick/jdbc/JdbcTypesComponent.scala#L15

// Official JDBC mapping docs
// https://docs.oracle.com/javase/tutorial/jdbc/basics/index.html
// https://docs.oracle.com/javase/1.5.0/docs/guide/jdbc/getstart/mapping.html#1055162
sealed trait MappedType[T] {
  def jdbcType: JDBCType
  def typeString: String = jdbcType.toString
  def nullable: Boolean = false
  def get(r: ResultSet, idx: Int): T
  def put(r: PreparedStatement, idx: Int, v: T): Unit
}
object MappedType {
  implicit object StringType extends MappedType[String] {
    def jdbcType = JDBCType.LONGVARCHAR
    def get(r: ResultSet, idx: Int) = r.getString(idx)
    def put(r: PreparedStatement, idx: Int, v: String) = r.setString(idx, v)
  }

  implicit object ByteType extends MappedType[Byte] {
    def jdbcType = JDBCType.TINYINT
    def get(r: ResultSet, idx: Int) = r.getByte(idx)
    def put(r: PreparedStatement, idx: Int, v: Byte) = r.setByte(idx, v)
  }

  implicit object ShortType extends MappedType[Short] {
    def jdbcType = JDBCType.SMALLINT
    def get(r: ResultSet, idx: Int) = r.getShort(idx)
    def put(r: PreparedStatement, idx: Int, v: Short) = r.setShort(idx, v)
  }

  implicit object IntType extends MappedType[Int] {
    def jdbcType = JDBCType.INTEGER
    def get(r: ResultSet, idx: Int) = r.getInt(idx)
    def put(r: PreparedStatement, idx: Int, v: Int) = r.setInt(idx, v)
  }

  implicit object LongType extends MappedType[Long] {
    def jdbcType = JDBCType.BIGINT
    def get(r: ResultSet, idx: Int) = r.getLong(idx)
    def put(r: PreparedStatement, idx: Int, v: Long) = r.setLong(idx, v)
  }

  implicit object DoubleType extends MappedType[Double] {
    def jdbcType = JDBCType.DOUBLE
    def get(r: ResultSet, idx: Int) = r.getDouble(idx)
    def put(r: PreparedStatement, idx: Int, v: Double) = r.setDouble(idx, v)
  }

  implicit object BigDecimalType extends MappedType[scala.math.BigDecimal] {
    def jdbcType = JDBCType.DOUBLE
    def get(r: ResultSet, idx: Int) = r.getBigDecimal(idx)
    def put(r: PreparedStatement, idx: Int, v: scala.math.BigDecimal) = r.setBigDecimal(idx, v.bigDecimal)
  }

  implicit object BooleanType extends MappedType[Boolean] {
    def jdbcType = JDBCType.BOOLEAN
    def get(r: ResultSet, idx: Int) = r.getBoolean(idx)
    def put(r: PreparedStatement, idx: Int, v: Boolean) = r.setBoolean(idx, v)
  }

  implicit object LocalDateType extends MappedType[LocalDate] {
    def jdbcType = JDBCType.DATE
    def get(r: ResultSet, idx: Int) = r.getObject(idx, classOf[LocalDate])
    def put(r: PreparedStatement, idx: Int, v: LocalDate) = r.setObject(idx, v)
  }

  implicit object LocalTimeType extends MappedType[LocalTime] {
    def jdbcType = JDBCType.TIME
    def get(r: ResultSet, idx: Int) = r.getObject(idx, classOf[LocalTime])
    def put(r: PreparedStatement, idx: Int, v: LocalTime) = r.setObject(idx, v)
  }

  implicit object LocalDateTimeType extends MappedType[LocalDateTime] {
    def jdbcType = JDBCType.TIMESTAMP
    def get(r: ResultSet, idx: Int) = r.getObject(idx, classOf[LocalDateTime])
    def put(r: PreparedStatement, idx: Int, v: LocalDateTime) = r.setObject(idx, v)
  }

  implicit object ZonedDateTimeType extends MappedType[ZonedDateTime] {
    def jdbcType = JDBCType.TIMESTAMP_WITH_TIMEZONE
    override def typeString = "TIMESTAMP WITH TIME ZONE"
    def get(r: ResultSet, idx: Int) = r.getTimestamp(idx).toInstant.atZone(ZoneId.systemDefault())
    def put(r: PreparedStatement, idx: Int, v: ZonedDateTime) = r
      .setTimestamp(idx, java.sql.Timestamp.from(v.toInstant))
  }

  implicit object InstantType extends MappedType[Instant] {
    def jdbcType = JDBCType.TIMESTAMP
    def get(r: ResultSet, idx: Int) = r.getTimestamp(idx).toInstant
    def put(r: PreparedStatement, idx: Int, v: Instant) = r
      .setTimestamp(idx, java.sql.Timestamp.from(v))
  }

  implicit object OffsetTimeType extends MappedType[OffsetTime] {
    def jdbcType = JDBCType.TIME_WITH_TIMEZONE
    override def typeString = "TIME WITH TIME ZONE"
    def get(r: ResultSet, idx: Int) = r.getObject(idx, classOf[OffsetTime])
    def put(r: PreparedStatement, idx: Int, v: OffsetTime) = r.setObject(idx, v)
  }

  implicit object OffsetDateTimeType extends MappedType[OffsetDateTime] {
    def jdbcType = JDBCType.TIMESTAMP_WITH_TIMEZONE
    override def typeString = "TIMESTAMP WITH TIME ZONE"

    def get(r: ResultSet, idx: Int) = {
      r.getTimestamp(idx).toInstant.atOffset(OffsetDateTime.now().getOffset)
    }
    def put(r: PreparedStatement, idx: Int, v: OffsetDateTime) = {
      r.setTimestamp(idx, java.sql.Timestamp.from(v.toInstant))
    }
  }

  implicit def OptionType[T](implicit inner: MappedType[T]): MappedType[Option[T]] =
    new MappedType[Option[T]] {
      def jdbcType: JDBCType = inner.jdbcType
      override def nullable = true
      def get(r: ResultSet, idx: Int): Option[T] = {
        if (r.getObject(idx) == null) None else Some(inner.get(r, idx))
      }

      def put(r: PreparedStatement, idx: Int, v: Option[T]): Unit = {
        v match {
          case None => r.setNull(idx, jdbcType.getVendorTypeNumber)
          case Some(value) => inner.put(r, idx, value)
        }
      }
    }
}
