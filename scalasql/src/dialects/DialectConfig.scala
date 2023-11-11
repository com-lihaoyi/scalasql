package scalasql.dialects

trait DialectConfig {
  def defaultQueryableSuffix: String
  def castParams: Boolean
}
