package object scalasql {

  /**
   * Convenience alias for `geny.Bytes`
   */
  def Bytes(x: String): geny.Bytes = new geny.Bytes(x.getBytes("UTF-8"))

  /**
   * Convenience alias for `geny.Bytes`
   */
  def Bytes(x: Array[Byte]): geny.Bytes = new geny.Bytes(x)

  /**
   * Convenience alias for `geny.Bytes`
   */
  type Bytes = geny.Bytes

  type Sc[T] = T

  val Table = query.Table
  type Table[V[_[_]]] = query.Table[V]

  val Column = query.Column
  type Column[T] = query.Column[T]

  val DbClient = core.DbClient
  type DbClient = core.DbClient

  val DbApi = core.DbApi
  type DbApi = core.DbApi

  val Queryable = core.Queryable
  type Queryable[Q, R] = core.Queryable[Q, R]

  val Expr = core.Expr
  type Expr[T] = core.Expr[T]

  type TypeMapper[T] = core.TypeMapper[T]
  val TypeMapper = core.TypeMapper

  val Config = core.Config
  type Config = core.Config

  val SqlStr = core.SqlStr
  type SqlStr = core.SqlStr

  val MySqlDialect = dialects.MySqlDialect
  type MySqlDialect = dialects.MySqlDialect

  val PostgresDialect = dialects.PostgresDialect
  type PostgresDialect = dialects.PostgresDialect

  val H2Dialect = dialects.H2Dialect
  type H2Dialect = dialects.H2Dialect

  val SqliteDialect = dialects.SqliteDialect
  type SqliteDialect = dialects.SqliteDialect
}
