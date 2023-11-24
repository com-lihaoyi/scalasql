package scalasql

/**
 * Things you to do to configure ScalaSql
 */
trait Config {
  def columnLabelPrefix = "res"
  def columnLabelDelimiter = "__"

  /**
   * Configures the underlying JDBC connection's `setFetchSize`
   */
  def defaultFetchSize: Int = -1

  /**
   * Configures the underlying JDBC connection's `setQueryTimeout`
   */
  def defaultQueryTimeoutSeconds: Int = -1

  /**
   * Translates table names from Scala `object` names to SQL names.
   */
  def tableNameMapper(v: String): String = Config.camelToSnake(v)

  /**
   * Translates column names from Scala `case class` field names to SQL names.
   */
  def columnNameMapper(v: String): String = Config.camelToSnake(v)
}

object Config {
  def joinName(chunks: List[String], config: Config) = {
    (config.columnLabelPrefix +: chunks).mkString(config.columnLabelDelimiter)
  }

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
