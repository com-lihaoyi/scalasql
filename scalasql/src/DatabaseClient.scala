package scalasql
import scalasql.dialects.DialectConfig

/**
 * A database client. Primarily allows you to access the database within a [[transaction]]
 * block or via [[autoCommit]]
 */
class DatabaseClient(
    connection: java.sql.Connection,
    config: Config,
    dialectConfig: DialectConfig
) {

  /**
   * Opens a database transaction within the given [[block]], automatically committing it
   * if the block returns successfully and rolling it back if the blow fails with an uncaught
   * exception. Within the block, you provides a [[DbTxn]] you can use to run queries, create
   * savepoints, or roll back the transaction.
   */
  def transaction[T](block: DbTxn => T): T = {
    connection.setAutoCommit(false)
    val txn = new DbApi.Impl(connection, config, dialectConfig, false, () => connection.rollback())
    try block(txn)
    catch {
      case e: Throwable =>
        connection.rollback()
        throw e
    } finally connection.setAutoCommit(true)
  }

  /**
   * Provides a [[DbApi]] that you can use to run queries in "auto-commit" mode, such
   * that every query runs in its own transaction and is committed automatically on-completion
   */
  def autoCommit: DbApi = {
    connection.setAutoCommit(true)
    new DbApi.Impl(connection, config, dialectConfig, autoCommit = true, () => ())
  }
}
