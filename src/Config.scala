package scalasql

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
    s.replaceAll("([A-Z])", "#$1").split('#').map(_.toLowerCase).mkString("_").stripPrefix("_")
  }

  def snakeToCamel(s: String) = {
    val out = new StringBuilder()
    val chunks = s.split("_", -1)
    for (i <- Range(0, chunks.length)) {
      val chunk = chunks(i)
      if (i == 0) out.append(chunk)
      else {
        out.append(chunk(0).toUpper)
        out.append(chunk.drop(1))
      }
    }
    out.toString()
  }
}
