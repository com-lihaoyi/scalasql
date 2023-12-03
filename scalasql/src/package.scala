package object scalasql {
  type Id[T] = T

  val Table = core.Table
  type Table[V[_[_]]] = core.Table[V]

  val DbClient = core.DbClient
  type DbClient = core.DbClient

  val DbApi = core.DbApi
  type DbApi = core.DbApi

  val Queryable = core.Queryable
  type Queryable[Q, R] = core.Queryable[Q, R]

  val Sql = core.Sql
  type Sql[T] = core.Sql[T]

  val Column = core.Column
  type Column[T] = core.Column[T]

  type TypeMapper[T] = core.TypeMapper[T]

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
