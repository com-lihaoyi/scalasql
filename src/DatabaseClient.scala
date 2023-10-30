package scalasql
import scalasql.dialects.DialectConfig

class DatabaseClient(
    connection: java.sql.Connection,
    config: Config,
    dialectConfig: DialectConfig
) {

  def transaction[T](block: DbApi => T): T = {
    connection.setAutoCommit(false)
    val txn = new DbApi(connection, config, dialectConfig, false, () => connection.rollback())
    try block(txn)
    catch {
      case e: Throwable =>
        connection.rollback()
        throw e
    } finally connection.setAutoCommit(true)
  }

  def autoCommit: DbApi = {
    connection.setAutoCommit(true)
    new DbApi(connection, config, dialectConfig, autoCommit = true, () => ())
  }
}
