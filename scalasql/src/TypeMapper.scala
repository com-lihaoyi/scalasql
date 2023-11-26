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

/**
 * A mapping between a Scala type [[T]] and a JDBC type, defined by
 * it's [[jdbcType]], [[typeString]], and [[get]] and [[put]] operations.
 *
 * Defaults are provided for most common Scala primitives, but you can also provide
 * your own by defining an `implicit val foo: TypeMapper[T]`
 */
trait TypeMapper[T] {
  def jdbcType: JDBCType
  def typeString: String = jdbcType.toString
  def get(r: ResultSet, idx: Int): T
  def put(r: PreparedStatement, idx: Int, v: T): Unit
}
object TypeMapper {

  /**
   * Type mapper for getting and setting objects to JDBC directly, without mapping.
   * Usually not what you want, so it's not implicitly available, but can be used
   * manually when necessary
   */
  object AnyType extends TypeMapper[Any] {
    def jdbcType = ???
    def get(r: ResultSet, idx: Int) = r.getObject(idx)
    def put(r: PreparedStatement, idx: Int, v: Any) = r.setObject(idx, v)
  }

  implicit object StringType extends TypeMapper[String] {
    def jdbcType = JDBCType.LONGVARCHAR
    def get(r: ResultSet, idx: Int) = r.getString(idx)
    def put(r: PreparedStatement, idx: Int, v: String) = r.setString(idx, v)
  }

  implicit object ByteType extends TypeMapper[Byte] {
    def jdbcType = JDBCType.TINYINT
    def get(r: ResultSet, idx: Int) = r.getByte(idx)
    def put(r: PreparedStatement, idx: Int, v: Byte) = r.setByte(idx, v)
  }

  implicit object ShortType extends TypeMapper[Short] {
    def jdbcType = JDBCType.SMALLINT
    def get(r: ResultSet, idx: Int) = r.getShort(idx)
    def put(r: PreparedStatement, idx: Int, v: Short) = r.setShort(idx, v)
  }

  implicit object IntType extends TypeMapper[Int] {
    def jdbcType = JDBCType.INTEGER
    def get(r: ResultSet, idx: Int) = r.getInt(idx)
    def put(r: PreparedStatement, idx: Int, v: Int) = r.setInt(idx, v)
  }

  implicit object LongType extends TypeMapper[Long] {
    def jdbcType = JDBCType.BIGINT
    def get(r: ResultSet, idx: Int) = r.getLong(idx)
    def put(r: PreparedStatement, idx: Int, v: Long) = r.setLong(idx, v)
  }

  implicit object DoubleType extends TypeMapper[Double] {
    def jdbcType = JDBCType.DOUBLE
    def get(r: ResultSet, idx: Int) = r.getDouble(idx)
    def put(r: PreparedStatement, idx: Int, v: Double) = r.setDouble(idx, v)
  }

  implicit object BigDecimalType extends TypeMapper[scala.math.BigDecimal] {
    def jdbcType = JDBCType.DOUBLE
    def get(r: ResultSet, idx: Int) = r.getBigDecimal(idx)
    def put(r: PreparedStatement, idx: Int, v: scala.math.BigDecimal) = r
      .setBigDecimal(idx, v.bigDecimal)
  }

  implicit object BooleanType extends TypeMapper[Boolean] {
    def jdbcType = JDBCType.BOOLEAN
    def get(r: ResultSet, idx: Int) = r.getBoolean(idx)
    def put(r: PreparedStatement, idx: Int, v: Boolean) = r.setBoolean(idx, v)
  }

  implicit object LocalDateType extends TypeMapper[LocalDate] {
    def jdbcType = JDBCType.DATE
    def get(r: ResultSet, idx: Int) = r.getObject(idx, classOf[LocalDate])
    def put(r: PreparedStatement, idx: Int, v: LocalDate) = r.setObject(idx, v)
  }

  implicit object LocalTimeType extends TypeMapper[LocalTime] {
    def jdbcType = JDBCType.TIME
    def get(r: ResultSet, idx: Int) = r.getObject(idx, classOf[LocalTime])
    def put(r: PreparedStatement, idx: Int, v: LocalTime) = r.setObject(idx, v)
  }

  implicit object LocalDateTimeType extends TypeMapper[LocalDateTime] {
    def jdbcType = JDBCType.TIMESTAMP
    def get(r: ResultSet, idx: Int) = r.getObject(idx, classOf[LocalDateTime])
    def put(r: PreparedStatement, idx: Int, v: LocalDateTime) = r.setObject(idx, v)
  }

  implicit object ZonedDateTimeType extends TypeMapper[ZonedDateTime] {
    def jdbcType = JDBCType.TIMESTAMP_WITH_TIMEZONE
    override def typeString = "TIMESTAMP WITH TIME ZONE"
    def get(r: ResultSet, idx: Int) = r.getTimestamp(idx).toInstant.atZone(ZoneId.systemDefault())
    def put(r: PreparedStatement, idx: Int, v: ZonedDateTime) = r
      .setTimestamp(idx, java.sql.Timestamp.from(v.toInstant))
  }

  implicit object InstantType extends TypeMapper[Instant] {
    def jdbcType = JDBCType.TIMESTAMP
    def get(r: ResultSet, idx: Int) = {
      r.getObject(idx) match {
        // Sqlite sometimes returns this
        case l: java.lang.Long => Instant.ofEpochMilli(l)
        // Sqlite sometimes also returns this
        case s: java.lang.String => java.sql.Timestamp.valueOf(s).toInstant
        // H2 and HsqlDb return this
        case o: java.time.OffsetDateTime => o.toInstant
        // MySql returns this
        case l: java.time.LocalDateTime =>
          l.toInstant(ZoneId.systemDefault().getRules().getOffset(Instant.now()))
        // Everyone seems to return this sometimes
        case l: java.sql.Timestamp => l.toInstant()
      }
    }
    def put(r: PreparedStatement, idx: Int, v: Instant) = r
      .setTimestamp(idx, java.sql.Timestamp.from(v))
  }

  implicit object OffsetTimeType extends TypeMapper[OffsetTime] {
    def jdbcType = JDBCType.TIME_WITH_TIMEZONE
    override def typeString = "TIME WITH TIME ZONE"
    def get(r: ResultSet, idx: Int) = r.getObject(idx, classOf[OffsetTime])
    def put(r: PreparedStatement, idx: Int, v: OffsetTime) = r.setObject(idx, v)
  }

  implicit object OffsetDateTimeType extends TypeMapper[OffsetDateTime] {
    def jdbcType = JDBCType.TIMESTAMP_WITH_TIMEZONE
    override def typeString = "TIMESTAMP WITH TIME ZONE"

    def get(r: ResultSet, idx: Int) = {
      r.getTimestamp(idx).toInstant.atOffset(OffsetDateTime.now().getOffset)
    }
    def put(r: PreparedStatement, idx: Int, v: OffsetDateTime) = {
      r.setTimestamp(idx, java.sql.Timestamp.from(v.toInstant))
    }
  }

  implicit def OptionType[T](implicit inner: TypeMapper[T]): TypeMapper[Option[T]] =
    new TypeMapper[Option[T]] {
      def jdbcType: JDBCType = inner.jdbcType
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
