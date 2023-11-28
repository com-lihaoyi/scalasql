package scalasql

import java.sql.{JDBCType, PreparedStatement, ResultSet, SQLType}

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