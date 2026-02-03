package scalasql.core

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
   * Returns a [[UseBlock]] for database transaction, automatically committing it
   * if the block returns successfully and rolling it back if the blow fails with an uncaught
   * exception. Within the block, you provides a [[DbApi.Txn]] you can use to run queries, create
   * savepoints, or roll back the transaction.
   */
  def transaction: UseBlock[DbApi.Txn]

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
      DbApi.renderSql(query, config, dialect.withCastParams(castParams))
    }

    lazy val transaction: UseBlock[DbApi.Txn] = UseBlockImpl[DbApi.Impl] {
      connection.setAutoCommit(false)
      val txn = new DbApi.Impl(connection, config, dialect, listeners, autoCommit = false)
      notifyListeners(txn.listeners)(_.begin())
      txn
    }((txn, error) => {
      error match {
        case None =>
          try {
            notifyListeners(txn.listeners)(_.beforeCommit())
          } catch {
            case beforeCommitHookErr: Throwable =>
              txn.rollbackCause(beforeCommitHookErr)
              throw beforeCommitHookErr
          }
          // this commits uncommitted operations, if any
          connection.setAutoCommit(true)
          // afterCommit exceptions just propagate - commit already done
          notifyListeners(txn.listeners)(_.afterCommit())

        case Some(useError) => txn.rollbackCause(useError)
      }
    })

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
      DbApi.renderSql(query, config, dialect.withCastParams(castParams))
    }

    private lazy val withConnectionImpl: UseBlockImpl[Connection] = UseBlockImpl
      .autoCloseable(dataSource.getConnection)
      .map(new Connection(_, config, listeners))

    lazy val withConnection: UseBlock[Connection] = withConnectionImpl

    lazy val transaction: UseBlock[DbApi.Txn] = withConnectionImpl.flatMap(_.transaction)

    def getAutoCommitClientConnection: DbApi = {
      val connection = dataSource.getConnection
      connection.setAutoCommit(true)
      new DbApi.Impl(connection, config, dialect, defaultListeners = Seq.empty, autoCommit = true)
    }
  }
}
