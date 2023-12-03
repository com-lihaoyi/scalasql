package scalasql.core

import scalasql.dialects.DialectConfig

/**
 * A database client. Primarily allows you to access the database within a [[transaction]]
 * block or via [[getAutoCommitClientConnection]]
 */
trait DatabaseClient {

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

object DatabaseClient {

  class Connection(
      connection: java.sql.Connection,
      config: Config,
      dialectConfig: DialectConfig
  ) extends DatabaseClient {

    def transaction[T](block: DbApi.Txn => T): T = {
      connection.setAutoCommit(false)
      val txn =
        new DbApi.Impl(connection, config, dialectConfig, false, () => connection.rollback())
      try block(txn)
      catch {
        case e: Throwable =>
          connection.rollback()
          throw e
      } finally connection.setAutoCommit(true)
    }

    def getAutoCommitClientConnection: DbApi = {
      connection.setAutoCommit(true)
      new DbApi.Impl(connection, config, dialectConfig, autoCommit = true, () => ())
    }
  }

  class DataSource(
      dataSource: javax.sql.DataSource,
      config: Config,
      dialectConfig: DialectConfig
  ) extends DatabaseClient {

    private def withConnection[T](f: DatabaseClient.Connection => T): T = {
      val connection = dataSource.getConnection
      try f(new DatabaseClient.Connection(connection, config, dialectConfig))
      finally connection.close()
    }

    def transaction[T](block: DbApi.Txn => T): T = withConnection(_.transaction(block))

    def getAutoCommitClientConnection: DbApi = {
      val connection = dataSource.getConnection
      connection.setAutoCommit(true)
      new DbApi.Impl(connection, config, dialectConfig, autoCommit = true, () => ())
    }
  }
}
