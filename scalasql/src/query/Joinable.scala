package scalasql.query

/**
 * Something that can be joined; typically a [[Select]] or a [[Table]]
 */
trait Joinable[Q, R] {
  protected def joinableSelect: Select[Q, R]
  protected def joinableIsTrivial: Boolean

  protected def joinableToFromExpr: (From, Q)
  def crossJoin[Q2, R2](): FlatJoin.Mapper[Q, Q2, R, R2] = {
    val (from, expr) = joinableToFromExpr
    new FlatJoin.Mapper[Q, Q2, R, R2]("CROSS JOIN", from, expr, None, Nil)
  }

  def join[Q2, R2](on: Q => Expr[Boolean]): FlatJoin.Mapper[Q, Q2, R, R2] = {
    val (from, expr) = joinableToFromExpr
    new FlatJoin.Mapper[Q, Q2, R, R2]("JOIN", from, expr, Some(on(expr)), Nil)
  }
  def leftJoin[Q2, R2](on: Q => Expr[Boolean]): FlatJoin.NullableMapper[Q, Q2, R, R2] = {
    val (from, expr) = joinableToFromExpr
    new FlatJoin.NullableMapper[Q, Q2, R, R2](
      "LEFT JOIN",
      from,
      JoinNullable(expr),
      Some(on(expr)),
      Nil
    )
  }

}
object Joinable {
  def joinableToFromExpr[Q, R](x: Joinable[Q, R]) = x.joinableToFromExpr
  def getSelect[Q, R](x: Joinable[Q, R]) = x.joinableSelect
  def getIsTrivial[Q, R](x: Joinable[Q, R]) = x.joinableIsTrivial
}
