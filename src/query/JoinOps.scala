package usql.query

import usql.{Queryable, Table}

trait JoinOps[C[_], Q] {
  def expr: Q
  def join[V](other: Joinable[V])(implicit qr: Queryable[V, _]): C[(Q, V)] = join0(other, None)

  def joinOn[V](other: Joinable[V])(on: (Q, V) => Expr[Boolean])(implicit
      qr: Queryable[V, _]
  ): C[(Q, V)] = join0(other, Some(on))

  def join0[V](other: Joinable[V], on: Option[(Q, V) => Expr[Boolean]])(implicit
      joinQr: Queryable[V, _]
  ): C[(Q, V)]

  def joinInfo[V](other: Joinable[V], on: Option[(Q, V) => Expr[Boolean]])(implicit
      joinQr: Queryable[V, _]
  ) = {
    val otherSelect = other.select

    val otherJoin =
      if (other.isTrivialJoin) Join(
        None,
        Seq(JoinFrom(
          otherSelect.asInstanceOf[SimpleSelect[_]].from.head,
          on.map(_(expr, otherSelect.expr))
        ))
      )
      else Join(
        None,
        Seq(JoinFrom(new SubqueryRef(otherSelect, joinQr), on.map(_(expr, otherSelect.expr))))
      )

    (Seq(otherJoin), otherSelect)
  }
}
