package scalasql
import scalasql.dialects.DialectConfig

class DatabaseClient(connection: java.sql.Connection, config: Config, dialectConfig: DialectConfig){
  var rolledBack = false

  def transaction[T](t: Txn => T): T  = {
    connection.setAutoCommit(false)
    val txn = new Txn(connection, config, dialectConfig, false, () => connection.rollback())
    try t(txn)
    catch {
      case e: Throwable =>
        connection.rollback()
        throw e
    } finally {
      rolledBack = false
      connection.setAutoCommit(true)
    }
  }

  def autoCommit: Txn = {
    connection.setAutoCommit(true)
    new Txn(connection, config, dialectConfig, autoCommit = true, () => ())
  }
}