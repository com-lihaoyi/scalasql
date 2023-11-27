package scalasql.query

import scalasql.{Queryable, Table}

trait JoinOps[C[_, _], Q, R] extends WithExpr[Q] {

  /**
   * Performs a `JOIN`/`INNER JOIN` on the given [[other]], typically a [[Table]] or [[Select]].
   */
  def join[Q2, R2, QF, RF](other: Joinable[Q2, R2])(on: (Q, Q2) => Expr[Boolean])(
    implicit ja: JoinAppend[Q, R, Q2, R2, QF, RF],
  ): C[QF, RF] = join0("JOIN", other, Some(on))

  /**
   * Performs a `CROSS JOIN`, which is an `INNER JOIN` but without the `ON` clause
   */
  def crossJoin[Q2, R2, QF, RF](other: Joinable[Q2, R2])(
      implicit ja: JoinAppend[Q, R, Q2, R2, QF, RF],
  ): C[QF, RF] = join0("CROSS JOIN", other, None)

  protected def join0[Q2, R2, QF, RF](
      prefix: String,
      other: Joinable[Q2, R2],
      on: Option[(Q, Q2) => Expr[Boolean]]
  )(
    implicit ja: JoinAppend[Q, R, Q2, R2, QF, RF],
  ): C[QF, RF]

  protected def joinInfo[Q2, R2](
      joinPrefix: String,
      other: Joinable[Q2, R2],
      on: Option[(Q, Q2) => Expr[Boolean]]
  )(implicit joinQr: Queryable.Row[Q2, _]) = {
    val otherSelect = Joinable.joinableSelect(other)

    val otherJoin = joinInfo0(
      joinPrefix,
      otherSelect,
      on.map(_(expr, otherSelect.expr)),
      Joinable.joinableIsTrivial(other)
    )

    (Seq(otherJoin), otherSelect)
  }
  protected def joinInfo0[Q2, R2](
      joinPrefix: String,
      otherSelect: Select[Q2, R2],
      on: Option[Expr[Boolean]],
      isTrivialJoin: Boolean
  )(implicit joinQr: Queryable.Row[Q2, _]) = {
    if (isTrivialJoin) {
      Join(
        joinPrefix,
        Seq(
          Join.From(
            otherSelect.asInstanceOf[SimpleSelect[_, _]].from.head,
            on
          )
        )
      )
    } else {
      Join(
        joinPrefix,
        Seq(
          Join.From(
            new SubqueryRef(otherSelect, joinQr.asInstanceOf[Queryable.Row[Q2, R2]]),
            on
          )
        )
      )
    }
  }

  def joinCopy2[Q2, R2, Q3, R3](
      other: Joinable[Q2, R2],
      on: Option[(Q, Q2) => Expr[Boolean]],
      joinPrefix: String
  )(
      f: (Q, Q2) => Q3
  )(implicit joinQr: Queryable.Row[Q2, _], jqr: Queryable.Row[Q3, R3]): SimpleSelect[Q3, R3] = {
    joinCopy[Q2, R2, Q3, R3](other, on, joinPrefix)(f)
  }

  protected def joinCopy[Q2, R2, Q3, R3](
      other: Joinable[Q2, R2],
      on: Option[(Q, Q2) => Expr[Boolean]],
      joinPrefix: String
  )(f: (Q, Q2) => Q3)(implicit joinQr: Queryable.Row[Q2, _], jqr: Queryable.Row[Q3, R3]) = {

    val (otherJoin, otherSelect) = joinInfo(joinPrefix, other, on)(joinQr)

    joinCopy0(f(expr, WithExpr.get(otherSelect)), otherJoin, Nil)(jqr)
  }

  protected def joinCopy0[Q3, R3](newExpr: Q3, newJoins: Seq[Join], newWheres: Seq[Expr[Boolean]])(
      implicit jqr: Queryable.Row[Q3, R3]
  ): SimpleSelect[Q3, R3] = ???
}

object JoinOps {
  def join0[C[_, _], Q, R, Q2, R2, QF, RF](
      v: JoinOps[C, Q, R],
      prefix: String,
      other: Joinable[Q2, R2],
      on: Option[(Q, Q2) => Expr[Boolean]]
  )(
    implicit ja: JoinAppend[Q, R, Q2, R2, QF, RF]
  ) = {
    v.join0[Q2, R2, QF, RF](prefix, other, on)
  }
}
