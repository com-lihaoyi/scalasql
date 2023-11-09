package scalasql.query

/**
 * Something that can be joined; typically a [[Select]] or a [[Table]]
 */
trait Joinable[Q, R] {
  def select: Select[Q, R]
  def isTrivialJoin: Boolean

  def toFromExpr: (From, Q)
  def join[Q2, R2](on: Q => Expr[Boolean]): FlatJoinMapper[Q, Q2, R, R2] = {
    val (from, expr) = toFromExpr
    new FlatJoinMapper[Q, Q2, R, R2](from, expr, on, Nil)
  }
}
