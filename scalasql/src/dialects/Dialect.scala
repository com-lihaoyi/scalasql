package scalasql.dialects

import scalasql.operations.{CaseWhen, DbApiOps, TableOps, WindowExpr}
import scalasql.query.Sql.apply0
import scalasql.query.{Aggregatable, Sql, JoinNullable, Select, WithCte, WithCteRef}
import scalasql.renderer.SqlStr
import scalasql.{DbApi, Queryable, Table, TypeMapper, operations}

import java.sql.{JDBCType, PreparedStatement, ResultSet}
import java.time.{
  Instant,
  LocalDate,
  LocalDateTime,
  LocalTime,
  OffsetDateTime,
  OffsetTime,
  ZoneId,
  ZonedDateTime
}
import java.util.UUID
import scala.reflect.ClassTag

/**
 * Base type for all SQL dialects. A Dialect proides extension methods, extension operators,
 * and custom implementations of various query classes that may differ between databases
 */
trait Dialect extends DialectConfig {
  implicit val dialectSelf: Dialect = this

  implicit def StringType: TypeMapper[String] = new StringType
  class StringType extends TypeMapper[String] {
    def jdbcType = JDBCType.LONGVARCHAR
    def get(r: ResultSet, idx: Int) = r.getString(idx)
    def put(r: PreparedStatement, idx: Int, v: String) = r.setString(idx, v)
  }

  implicit def ByteType: TypeMapper[Byte] = new ByteType
  class ByteType extends TypeMapper[Byte] {
    def jdbcType = JDBCType.TINYINT
    def get(r: ResultSet, idx: Int) = r.getByte(idx)
    def put(r: PreparedStatement, idx: Int, v: Byte) = r.setByte(idx, v)
  }

  implicit def ShortType: TypeMapper[Short] = new ShortType
  class ShortType extends TypeMapper[Short] {
    def jdbcType = JDBCType.SMALLINT
    def get(r: ResultSet, idx: Int) = r.getShort(idx)
    def put(r: PreparedStatement, idx: Int, v: Short) = r.setShort(idx, v)
  }

  implicit def IntType: TypeMapper[Int] = new IntType
  class IntType extends TypeMapper[Int] {
    def jdbcType = JDBCType.INTEGER
    def get(r: ResultSet, idx: Int) = r.getInt(idx)
    def put(r: PreparedStatement, idx: Int, v: Int) = r.setInt(idx, v)
  }

  implicit def LongType: TypeMapper[Long] = new LongType
  class LongType extends TypeMapper[Long] {
    def jdbcType = JDBCType.BIGINT
    def get(r: ResultSet, idx: Int) = r.getLong(idx)
    def put(r: PreparedStatement, idx: Int, v: Long) = r.setLong(idx, v)
  }

  implicit def DoubleType: TypeMapper[Double] = new DoubleType
  class DoubleType extends TypeMapper[Double] {
    def jdbcType = JDBCType.DOUBLE
    def get(r: ResultSet, idx: Int) = r.getDouble(idx)
    def put(r: PreparedStatement, idx: Int, v: Double) = r.setDouble(idx, v)
  }

  implicit def BigDecimalType: TypeMapper[scala.math.BigDecimal] = new BigDecimalType
  class BigDecimalType extends TypeMapper[scala.math.BigDecimal] {
    def jdbcType = JDBCType.DOUBLE
    def get(r: ResultSet, idx: Int) = r.getBigDecimal(idx)
    def put(r: PreparedStatement, idx: Int, v: scala.math.BigDecimal) = r
      .setBigDecimal(idx, v.bigDecimal)
  }

  implicit def BooleanType: TypeMapper[Boolean] = new BooleanType
  class BooleanType extends TypeMapper[Boolean] {
    def jdbcType = JDBCType.BOOLEAN
    def get(r: ResultSet, idx: Int) = r.getBoolean(idx)
    def put(r: PreparedStatement, idx: Int, v: Boolean) = r.setBoolean(idx, v)
  }

  implicit def UuidType: TypeMapper[UUID] = new UuidType
  class UuidType extends TypeMapper[UUID] {
    def jdbcType = JDBCType.VARBINARY

    def get(r: ResultSet, idx: Int) = {
      r.getObject(idx) match {
        case u: UUID => u
        case s: String => UUID.fromString(s)
      }
    }

    def put(r: PreparedStatement, idx: Int, v: UUID) = {
      r.setObject(idx, v)
    }
  }

  implicit def BytesType: TypeMapper[geny.Bytes] = new BytesType
  class BytesType extends TypeMapper[geny.Bytes] {
    def jdbcType = JDBCType.VARBINARY
    def get(r: ResultSet, idx: Int) = new geny.Bytes(r.getBytes(idx))
    def put(r: PreparedStatement, idx: Int, v: geny.Bytes) = r.setBytes(idx, v.array)
  }

  implicit def LocalDateType: TypeMapper[LocalDate] = new LocalDateType
  class LocalDateType extends TypeMapper[LocalDate] {
    def jdbcType = JDBCType.DATE
    def get(r: ResultSet, idx: Int) = r.getObject(idx, classOf[LocalDate])
    def put(r: PreparedStatement, idx: Int, v: LocalDate) = r.setObject(idx, v)
  }

  implicit def LocalTimeType: TypeMapper[LocalTime] = new LocalTimeType
  class LocalTimeType extends TypeMapper[LocalTime] {
    def jdbcType = JDBCType.TIME
    def get(r: ResultSet, idx: Int) = r.getObject(idx, classOf[LocalTime])
    def put(r: PreparedStatement, idx: Int, v: LocalTime) = r.setObject(idx, v)
  }

  implicit def LocalDateTimeType: TypeMapper[LocalDateTime] = new LocalDateTimeType
  class LocalDateTimeType extends TypeMapper[LocalDateTime] {
    def jdbcType = JDBCType.TIMESTAMP
    def get(r: ResultSet, idx: Int) = r.getObject(idx, classOf[LocalDateTime])
    def put(r: PreparedStatement, idx: Int, v: LocalDateTime) = r.setObject(idx, v)
  }

  implicit def ZonedDateTimeType: TypeMapper[ZonedDateTime] = new ZonedDateTimeType
  class ZonedDateTimeType extends TypeMapper[ZonedDateTime] {
    def jdbcType = JDBCType.TIMESTAMP_WITH_TIMEZONE
    override def castTypeString = "TIMESTAMP WITH TIME ZONE"
    def get(r: ResultSet, idx: Int) = r.getTimestamp(idx).toInstant.atZone(ZoneId.systemDefault())
    def put(r: PreparedStatement, idx: Int, v: ZonedDateTime) = r
      .setTimestamp(idx, java.sql.Timestamp.from(v.toInstant))
  }

  implicit def InstantType: TypeMapper[Instant] = new InstantType
  class InstantType extends TypeMapper[Instant] {
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

  implicit def OffsetTimeType: TypeMapper[OffsetTime] = new OffsetTimeType
  class OffsetTimeType extends TypeMapper[OffsetTime] {
    def jdbcType = JDBCType.TIME_WITH_TIMEZONE
    override def castTypeString = "TIME WITH TIME ZONE"
    def get(r: ResultSet, idx: Int) = r.getObject(idx, classOf[OffsetTime])
    def put(r: PreparedStatement, idx: Int, v: OffsetTime) = r.setObject(idx, v)
  }

  implicit def OffsetDateTimeType: TypeMapper[OffsetDateTime] = new OffsetDateTimeType
  class OffsetDateTimeType extends TypeMapper[OffsetDateTime] {
    def jdbcType = JDBCType.TIMESTAMP_WITH_TIMEZONE
    override def castTypeString = "TIMESTAMP WITH TIME ZONE"
    def get(r: ResultSet, idx: Int) = {
      r.getTimestamp(idx).toInstant.atOffset(OffsetDateTime.now().getOffset)
    }
    def put(r: PreparedStatement, idx: Int, v: OffsetDateTime) = {
      r.setTimestamp(idx, java.sql.Timestamp.from(v.toInstant))
    }
  }

  implicit def EnumType[T <: Enumeration#Value](implicit constructor: String => T): TypeMapper[T] =
    new EnumType[T]
  class EnumType[T](implicit constructor: String => T) extends TypeMapper[T] {
    def jdbcType: JDBCType = JDBCType.VARCHAR
    def get(r: ResultSet, idx: Int): T = constructor(r.getString(idx))
    def put(r: PreparedStatement, idx: Int, v: T) = r.setObject(idx, v, java.sql.Types.OTHER)
  }

  implicit def from(x: Int): Sql[Int] = Sql(x)

  implicit def from(x: Long): Sql[Long] = Sql(x)

  implicit def from(x: Boolean): Sql[Boolean] = Sql.apply0(x, x)

  implicit def from(x: Double): Sql[Double] = Sql(x)

  implicit def from(x: scala.math.BigDecimal): Sql[scala.math.BigDecimal] = Sql(x)

  implicit def from(x: String): Sql[String] = Sql(x)

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
  implicit def ExprBooleanOpsConv(v: Sql[Boolean]): operations.ExprBooleanOps =
    new operations.ExprBooleanOps(v)
  implicit def ExprNumericOpsConv[T: Numeric: TypeMapper](
      v: Sql[T]
  ): operations.ExprNumericOps[T] = new operations.ExprNumericOps(v)

  implicit def ExprOpsConv(v: Sql[_]): operations.ExprOps = new operations.ExprOps(v)

  implicit def ExprTypedOpsConv[T: ClassTag](v: Sql[T]): operations.ExprTypedOps[T] =
    new operations.ExprTypedOps(v)

  implicit def ExprOptionOpsConv[T: TypeMapper](v: Sql[Option[T]]): operations.ExprOptionOps[T] =
    new operations.ExprOptionOps(v)

  implicit def NullableExprOpsConv[T: TypeMapper](v: JoinNullable[Sql[T]]): operations.ExprOps =
    new operations.ExprOps(JoinNullable.toExpr(v))

  implicit def NullableExprOptionOpsConv[T: TypeMapper](
      v: JoinNullable[Sql[T]]
  ): operations.ExprOptionOps[T] =
    new operations.ExprOptionOps(JoinNullable.toExpr(v))

  implicit def ExprStringOpsConv(v: Sql[String]): operations.ExprStringOps

  implicit def AggNumericOpsConv[V: Numeric: TypeMapper](v: Aggregatable[Sql[V]])(
      implicit qr: Queryable.Row[Sql[V], V]
  ): operations.AggNumericOps[V] = new operations.AggNumericOps(v)

  implicit def AggOpsConv[T](v: Aggregatable[T])(
      implicit qr: Queryable.Row[T, _]
  ): operations.AggOps[T] = new operations.AggOps(v)

  implicit def AggExprOpsConv[T](v: Aggregatable[Sql[T]]): operations.AggExprOps[T]

  implicit def TableOpsConv[V[_[_]]](t: Table[V]): TableOps[V] = new TableOps(t)
  implicit def DbApiOpsConv(db: => DbApi): DbApiOps = new DbApiOps(this)

  implicit class WindowExtensions[T](e: Sql[T]) {
    def over = new WindowExpr[T](e, None, None, Nil, None, None, None)
  }
  // This is necessary for `runSql` to work.
  implicit def ExprQueryable[T](implicit mt: TypeMapper[T]): Queryable.Row[Sql[T], T] = {
    new Sql.ExprQueryable[Sql, T]()
  }
}
