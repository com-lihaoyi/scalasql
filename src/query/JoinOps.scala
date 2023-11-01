package scalasql.query

import scalasql.{Queryable, Table}

trait JoinOps[C[_, _], Q, R] {
  def expr: Q
  def join[Q2, R2](other: Joinable[Q2, R2])(implicit qr: Queryable[Q2, R2]): C[(Q, Q2), (R, R2)] =
    join0(other, None)

  def joinOn[Q2, R2](other: Joinable[Q2, R2])(on: (Q, Q2) => Expr[Boolean])(
      implicit qr: Queryable[Q2, R2]
  ): C[(Q, Q2), (R, R2)] = join0(other, Some(on))

  def join0[Q2, R2](other: Joinable[Q2, R2], on: Option[(Q, Q2) => Expr[Boolean]])(
      implicit joinQr: Queryable[Q2, R2]
  ): C[(Q, Q2), (R, R2)]

  def leftJoin[Q2, R2](other: Joinable[Q2, R2])(on: (Q, Q2) => Expr[Boolean])(
      implicit qr: Queryable[Q2, R2]
  ): C[(Q, Option[Q2]), (R, Option[R2])] = leftJoin0(other, Some(on))

  def leftJoin0[Q2, R2](other: Joinable[Q2, R2], on: Option[(Q, Q2) => Expr[Boolean]])(
      implicit joinQr: Queryable[Q2, R2]
  ): C[(Q, Option[Q2]), (R, Option[R2])]

  def rightJoin[Q2, R2](other: Joinable[Q2, R2])(on: (Q, Q2) => Expr[Boolean])(
      implicit qr: Queryable[Q2, R2]
  ): C[(Option[Q], Q2), (Option[R], R2)] = rightJoin0(other, Some(on))

  def rightJoin0[Q2, R2](other: Joinable[Q2, R2], on: Option[(Q, Q2) => Expr[Boolean]])(
      implicit joinQr: Queryable[Q2, R2]
  ): C[(Option[Q], Q2), (Option[R], R2)]

  def joinInfo[Q2, R2](joinPrefix: Option[String],
                       other: Joinable[Q2, R2],
                       on: Option[(Q, Q2) => Expr[Boolean]])(
      implicit joinQr: Queryable[Q2, _]
  ) = {
    val otherSelect = other.select

    val otherJoin =
      if (other.isTrivialJoin) Join(
        joinPrefix,
        Seq(JoinFrom(
          otherSelect.asInstanceOf[SimpleSelect[_, _]].from.head,
          on.map(_(expr, otherSelect.expr))
        ))
      )
      else Join(
        joinPrefix,
        Seq(JoinFrom(new SubqueryRef(otherSelect, joinQr.asInstanceOf[Queryable[Q2, R2]]), on.map(_(expr, otherSelect.expr))))
      )

    (Seq(otherJoin), otherSelect)
  }
}
