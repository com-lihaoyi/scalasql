package scalasql.query

/**
 * Something that can be joined; typically a [[Select]] or a [[Table]]
 */
trait Joinable[Q, R] {
  protected def joinableSelect: Select[Q, R]
  protected def joinableIsTrivial: Boolean

  protected def joinableToFromExpr: (From, Q)
  def join[Q2, R2](on: Q => Expr[Boolean]): FlatJoin.Mapper[Q, Q2, R, R2] = {
    val (from, expr) = joinableToFromExpr
    new FlatJoin.Mapper[Q, Q2, R, R2](from, expr, on(expr), Nil)
  }
}
object Joinable {
  def getSelect[Q, R](x: Joinable[Q, R]) = x.joinableSelect
  def getIsTrivial[Q, R](x: Joinable[Q, R]) = x.joinableIsTrivial
}
