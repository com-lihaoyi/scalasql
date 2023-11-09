package scalasql.query

/**
 * Something that can be joined; typically a [[Select]] or a [[Table]]
 */
trait Joinable[Q, R] {
  def select: Select[Q, R]
  def isTrivialJoin: Boolean

  def expr: Q
  def joinX[Q2, R2](on: Q => Expr[Boolean]): FlatJoinMapper[Q, Q2, R, R2] = {
    new FlatJoinMapper[Q, Q2, R, R2](this, expr, on(expr))
  }
}
