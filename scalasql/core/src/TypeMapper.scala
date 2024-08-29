package scalasql.core

import java.sql.{JDBCType, PreparedStatement, ResultSet}
import java.time.{
  LocalDate,
  LocalTime,
  LocalDateTime,
  ZonedDateTime,
  Instant,
  OffsetTime,
  OffsetDateTime
}
import java.util.UUID

// What Quill does
// https://github.com/zio/zio-quill/blob/43ee1dab4f717d7e6683aa24c391740f3d17df50/quill-jdbc/src/main/scala/io/getquill/context/jdbc/Encoders.scala#L104

// What SLICK does
// https://github.com/slick/slick/blob/88b2ffb177776fd74dee38124b8c54d616d1a9ae/slick/src/main/scala/slick/jdbc/JdbcTypesComponent.scala#L15

// Official JDBC mapping docs
// https://docs.oracle.com/javase/tutorial/jdbc/basics/index.html
// https://docs.oracle.com/javase/1.5.0/docs/guide/jdbc/getstart/mapping.html#1055162

/**
 * A mapping between a Scala type [[T]] and a JDBC type, defined by
 * it's [[jdbcType]], [[castTypeString]], and [[get]] and [[put]] operations.
 *
 * Defaults are provided for most common Scala primitives, but you can also provide
 * your own by defining an `implicit val foo: TypeMapper[T]`
 */
trait TypeMapper[T] { outer =>

  /**
   * The JDBC type of this type. Used for `setNull` which needs to know the
   * `java.sql.Types` integer ID of the type to set it properly
   */
  def jdbcType: JDBCType

  /**
   * What SQL string to use when you run `cast[T]` to a specific type
   */
  def castTypeString: String = jdbcType.toString

  /**
   * How to extract a value of type [[T]] from a `ResultSet`
   */
  def get(r: ResultSet, idx: Int): T

  /**
   * How to insert a value of type [[T]] into a `PreparedStatement`
   */
  def put(r: PreparedStatement, idx: Int, v: T): Unit

  /**
   * Create a new `TypeMapper[V]` based on this `TypeMapper[T]` given the
   * two conversion functions `f: V => T`, `g: T => V`
   */
  def bimap[V](f: V => T, g: T => V): TypeMapper[V] = new TypeMapper[V] {
    def jdbcType: JDBCType = outer.jdbcType
    override def castTypeString: String = outer.castTypeString
    def get(r: ResultSet, idx: Int): V = g(outer.get(r, idx))
    def put(r: PreparedStatement, idx: Int, v: V): Unit = outer.put(r, idx, f(v))
  }
}

object TypeMapper {

  /**
   * These definitions are workarounds for a bug in the Scala 3 compiler
   * https://github.com/scala/scala3/issues/19436
   *
   * The `TableMacros` definition in Scala 3 could ideally just `import dialect.*` to get the
   * `TypeMapper` instances in scope, but it triggers a crash similar to the one in the bug report.
   *
   * Instead, the macro declares a local `given d: DialectTypeMappers = dialect` and relies on these
   * implicits to summon the necessary instances.
   */
  implicit def stringFromDialectTypeMappers(implicit d: DialectTypeMappers): TypeMapper[String] =
    d.StringType
  implicit def byteFromDialectTypeMappers(implicit d: DialectTypeMappers): TypeMapper[Byte] =
    d.ByteType
  implicit def shortFromDialectTypeMappers(implicit d: DialectTypeMappers): TypeMapper[Short] =
    d.ShortType
  implicit def intFromDialectTypeMappers(implicit d: DialectTypeMappers): TypeMapper[Int] =
    d.IntType
  implicit def longFromDialectTypeMappers(implicit d: DialectTypeMappers): TypeMapper[Long] =
    d.LongType

  implicit def doubleFromDialectTypeMappers(implicit d: DialectTypeMappers): TypeMapper[Double] =
    d.DoubleType
  implicit def bigDecimalFromDialectTypeMappers(
      implicit d: DialectTypeMappers
  ): TypeMapper[scala.math.BigDecimal] = d.BigDecimalType
  implicit def booleanFromDialectTypeMappers(implicit d: DialectTypeMappers): TypeMapper[Boolean] =
    d.BooleanType
  implicit def uuidFromDialectTypeMappers(implicit d: DialectTypeMappers): TypeMapper[UUID] =
    d.UuidType
  implicit def bytesFromDialectTypeMappers(implicit d: DialectTypeMappers): TypeMapper[geny.Bytes] =
    d.BytesType
  implicit def utilDateFromDialectTypeMappers(
      implicit d: DialectTypeMappers
  ): TypeMapper[java.util.Date] = d.UtilDateType
  implicit def localDateFromDialectTypeMappers(
      implicit d: DialectTypeMappers
  ): TypeMapper[LocalDate] = d.LocalDateType
  implicit def localTimeFromDialectTypeMappers(
      implicit d: DialectTypeMappers
  ): TypeMapper[LocalTime] = d.LocalTimeType

  implicit def localDateTimeFromDialectTypeMappers(
      implicit d: DialectTypeMappers
  ): TypeMapper[LocalDateTime] = d.LocalDateTimeType

  implicit def zonedDateTimeFromDialectTypeMappers(
      implicit d: DialectTypeMappers
  ): TypeMapper[ZonedDateTime] = d.ZonedDateTimeType
  implicit def instantFromDialectTypeMappers(implicit d: DialectTypeMappers): TypeMapper[Instant] =
    d.InstantType

  implicit def offsetTimeFromDialectTypeMappers(
      implicit d: DialectTypeMappers
  ): TypeMapper[OffsetTime] = d.OffsetTimeType

  implicit def offsetDateTimeFromDialectTypeMappers(
      implicit d: DialectTypeMappers
  ): TypeMapper[OffsetDateTime] = d.OffsetDateTimeType
  implicit def enumTypeFromDialectTypeMappers[T <: Enumeration#Value](
      implicit d: DialectTypeMappers,
      constructor: String => T
  ): TypeMapper[T] = d.EnumType[T]
  implicit def optionTypeFromDialectTypeMappers[T](
      implicit d: DialectTypeMappers,
      inner: TypeMapper[T]
  ): TypeMapper[Option[T]] = d.OptionType[T]
}
