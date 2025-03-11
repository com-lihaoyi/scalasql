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

  /**
   * Calls the given function for each listener, collecting any exceptions and throwing them
   * as a single exception if any are thrown.
   */
  private[core] def notifyListeners(listeners: Iterable[DbApi.TransactionListener])(
      f: DbApi.TransactionListener => Unit
  ): Unit = {
    if (listeners.isEmpty) return

    var exception: Throwable = null
    listeners.foreach { listener =>
      try {
        f(listener)
      } catch {
        case e: Throwable =>
          if (exception == null) exception = e
          else exception.addSuppressed(e)
      }
    }
    if (exception != null) throw exception
  }

  class Connection(
      connection: java.sql.Connection,
      config: Config = new Config {},
      /** Listeners that are added to all transactions created by this connection */
      listeners: Seq[DbApi.TransactionListener] = Seq.empty
  )(implicit dialect: DialectConfig)
      extends DbClient {

    def renderSql[Q, R](query: Q, castParams: Boolean = false)(
        implicit qr: Queryable[Q, R]
    ): String = {
      DbApi.renderSql(query, config, castParams)
    }

    def transaction[T](block: DbApi.Txn => T): T = {
      connection.setAutoCommit(false)
      val txn = new DbApi.Impl(connection, config, dialect, listeners, autoCommit = false)
      var rolledBack = false
      try {
        notifyListeners(txn.listeners)(_.begin())
        val result = block(txn)
        notifyListeners(txn.listeners)(_.beforeCommit())
        result
      } catch {
        case e: Throwable =>
          rolledBack = true
          try {
            notifyListeners(txn.listeners)(_.beforeRollback())
          } catch {
            case e2: Throwable => e.addSuppressed(e2)
          } finally {
            connection.rollback()
            try {
              notifyListeners(txn.listeners)(_.afterRollback())
            } catch {
              case e3: Throwable => e.addSuppressed(e3)
            }
          }
          throw e
      } finally {
        // this commits uncommitted operations, if any
        connection.setAutoCommit(true)
        if (!rolledBack) {
          notifyListeners(txn.listeners)(_.afterCommit())
        }
      }
    }

    def getAutoCommitClientConnection: DbApi = {
      connection.setAutoCommit(true)
      new DbApi.Impl(connection, config, dialect, listeners, autoCommit = true)
    }
  }

  class DataSource(
      dataSource: javax.sql.DataSource,
      config: Config = new Config {},
      /** Listeners that are added to all transactions created through the [[DataSource]] */
      listeners: Seq[DbApi.TransactionListener] = Seq.empty
  )(implicit dialect: DialectConfig)
      extends DbClient {

    /** Returns a new [[DataSource]] with the given listener added */
    def withTransactionListener(listener: DbApi.TransactionListener): DbClient = {
      new DataSource(dataSource, config, listeners :+ listener)
    }

    def renderSql[Q, R](query: Q, castParams: Boolean = false)(
        implicit qr: Queryable[Q, R]
    ): String = {
      DbApi.renderSql(query, config, castParams)
    }

    private def withConnection[T](f: DbClient.Connection => T): T = {
      val connection = dataSource.getConnection
      try f(new DbClient.Connection(connection, config, listeners))
      finally connection.close()
    }

    def transaction[T](block: DbApi.Txn => T): T = withConnection(_.transaction(block))

    def getAutoCommitClientConnection: DbApi = {
      val connection = dataSource.getConnection
      connection.setAutoCommit(true)
      new DbApi.Impl(connection, config, dialect, defaultListeners = Seq.empty, autoCommit = true)
    }
  }
}
