package scalasql.core

/**
 * Things you to do to configure ScalaSql
 */
trait Config {
  def columnLabelDefault = "res"
  def columnLabelDelimiter = "_"

  /**
   * Configures the underlying JDBC connection's `setFetchSize`
   */
  def defaultFetchSize: Int = -1

  /**
   * Configures the underlying JDBC connection's `setQueryTimeout`
   */
  def defaultQueryTimeoutSeconds: Int = -1

  /**
   * Translates table and column names from Scala `object` names to SQL names.
   *
   * Use [[tableNameMapper]] and [[columnNameMapper]] if you want different
   * translations for table and column names
   */
  def nameMapper(v: String): String = Config.camelToSnake(v)

  /**
   * Translates table names from Scala `object` names to SQL names.
   */
  def tableNameMapper(v: String): String = nameMapper(v)

  /**
   * Translates column names from Scala `case class` field names to SQL names.
   */
  def columnNameMapper(v: String): String = nameMapper(v)

  /**
   * Override this to log the executed SQL queries
   */
  def logSql(sql: String, file: String, line: Int): Unit = ()
}

object Config {
  def camelToSnake(s: String) = {
    val chars = new collection.mutable.StringBuilder
    var lowercase = false
    for (c <- s) {
      if (c.isUpper) {
        if (lowercase == true) chars.append('_')
        chars.append(c.toLower)
        lowercase = false
      } else {
        chars.append(c)
        lowercase = true
      }

    }
    chars.toString()
  }
}
