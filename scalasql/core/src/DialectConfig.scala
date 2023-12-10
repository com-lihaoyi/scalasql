package scalasql.core

trait DialectConfig {
  protected def dialectCastParams: Boolean
}

object DialectConfig {
  def castParams(d: DialectConfig) = d.dialectCastParams
}
