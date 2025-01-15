package scalasql.core

trait DialectConfig {
  protected def dialectCastParams: Boolean
  protected def supportSavepointRelease: Boolean
}

object DialectConfig {
  def castParams(d: DialectConfig) = d.dialectCastParams
  def supportSavepointRelease(d: DialectConfig) = d.supportSavepointRelease
}
