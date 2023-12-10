package scalasql.core

import scalasql.core.DialectConfig

/**
 * A database client. Primarily allows you to access the database within a [[transaction]]
 * block or via [[getAutoCommitClientConnection]]
 */
trait DbClient {

  /**
   * Converts the given query [[Q]] into a string. Useful for debugging and logging
   */
  def renderSql[Q, R](query: Q, castParams: Boolean = false)(implicit qr: Queryable[Q, R]): String

  /**
   * Opens a database transaction within the given [[block]], automatically committing it
   * if the block returns successfully and rolling it back if the blow fails with an uncaught
   * exception. Within the block, you provides a [[DbApi.Txn]] you can use to run queries, create
   * savepoints, or roll back the transaction.
   */
  def transaction[T](block: DbApi.Txn => T): T

  /**
   * Provides a [[DbApi]] that you can use to run queries in "auto-commit" mode, such
   * that every query runs in its own transaction and is committed automatically on-completion.
   *
   * This can be useful for interactive testing, but requires that you manually manage the
   * closing of the connection to avoid leaking connections (if using a connection pool like
   * HikariCP), and should be avoided in most production environments in favor of
   * `.transaction{...}` blocks.
   */
  def getAutoCommitClientConnection: DbApi
}

object DbClient {

  class Connection(
      connection: java.sql.Connection,
      config: Config = new Config {}
  )(implicit dialect: DialectConfig)
      extends DbClient {

    def renderSql[Q, R](query: Q, castParams: Boolean = false)(
        implicit qr: Queryable[Q, R]
    ): String = {
      DbApi.renderSql(query, config, castParams)
    }

    def transaction[T](block: DbApi.Txn => T): T = {
      connection.setAutoCommit(false)
      val txn =
        new DbApi.Impl(connection, config, dialect, false, () => connection.rollback())
      try block(txn)
      catch {
        case e: Throwable =>
          connection.rollback()
          throw e
      } finally connection.setAutoCommit(true)
    }

    def getAutoCommitClientConnection: DbApi = {
      connection.setAutoCommit(true)
      new DbApi.Impl(connection, config, dialect, autoCommit = true, () => ())
    }
  }

  class DataSource(
      dataSource: javax.sql.DataSource,
      config: Config = new Config {}
  )(implicit dialect: DialectConfig)
      extends DbClient {

    def renderSql[Q, R](query: Q, castParams: Boolean = false)(
        implicit qr: Queryable[Q, R]
    ): String = {
      DbApi.renderSql(query, config, castParams)
    }

    private def withConnection[T](f: DbClient.Connection => T): T = {
      val connection = dataSource.getConnection
      try f(new DbClient.Connection(connection, config))
      finally connection.close()
    }

    def transaction[T](block: DbApi.Txn => T): T = withConnection(_.transaction(block))

    def getAutoCommitClientConnection: DbApi = {
      val connection = dataSource.getConnection
      connection.setAutoCommit(true)
      new DbApi.Impl(connection, config, dialect, autoCommit = true, () => ())
    }
  }
}
