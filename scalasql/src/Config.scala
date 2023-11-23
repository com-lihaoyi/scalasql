package scalasql

/**
 * Things you to do to configure ScalaSql
 */
trait Config {
  def columnLabelPrefix = "res"
  def columnLabelDelimiter = "__"
  def defaultFetchSize: Int = -1
  def defaultQueryTimeoutSeconds: Int = -1

  def tableNameMapper(v: String): String = Config.camelToSnake(v)
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
