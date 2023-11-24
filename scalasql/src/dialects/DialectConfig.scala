package scalasql.dialects

trait DialectConfig {
  protected def dialectCastParams: Boolean
}
object DialectConfig {
  def dialectCastParams(d: DialectConfig) = d.dialectCastParams
}
