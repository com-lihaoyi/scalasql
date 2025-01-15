package scalasql.core

trait DialectConfig { that =>
  def castParams: Boolean
  def escape(str: String): String
  def supportSavepointRelease: Boolean

  def withCastParams(params: Boolean) = new DialectConfig {
    def castParams: Boolean = params

    def supportSavepointRelease = that.supportSavepointRelease

    def escape(str: String): String = that.escape(str)
  }
}
