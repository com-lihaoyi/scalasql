package scalasql.query

import scalasql.{Queryable, Table}

trait JoinOps[C[_, _], Q, R] {
  def expr: Q

  /**
   * Performs a `JOIN`/`INNER JOIN` on the given [[other]], typically a [[Table]] or [[Select]].
   */
  def join[Q2, R2](other: Joinable[Q2, R2])(on: (Q, Q2) => Expr[Boolean])(
      implicit qr: Queryable.Row[Q2, R2]
  ): C[(Q, Q2), (R, R2)] = join0(other, Some(on))

  protected def join0[Q2, R2](other: Joinable[Q2, R2], on: Option[(Q, Q2) => Expr[Boolean]])(
      implicit joinQr: Queryable.Row[Q2, R2]
  ): C[(Q, Q2), (R, R2)]

  protected def joinInfo[Q2, R2](
      joinPrefix: Option[String],
      other: Joinable[Q2, R2],
      on: Option[(Q, Q2) => Expr[Boolean]]
  )(implicit joinQr: Queryable.Row[Q2, _]) = {
    val otherSelect = other.select

    val otherJoin =
      if (other.isTrivialJoin)
        Join(
          joinPrefix,
          Seq(
            Join.From(
              otherSelect.asInstanceOf[SimpleSelect[_, _]].from.head,
              on.map(_(expr, otherSelect.expr))
            )
          )
        )
      else
        Join(
          joinPrefix,
          Seq(
            Join.From(
              new SubqueryRef(otherSelect, joinQr.asInstanceOf[Queryable.Row[Q2, R2]]),
              on.map(_(expr, otherSelect.expr))
            )
          )
        )

    (Seq(otherJoin), otherSelect)
  }
}
