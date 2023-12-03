package scalasql.query

import scalasql.core.{Queryable, Sql, SubqueryRef, Table, WithExpr}

trait JoinOps[C[_, _], Q, R] extends WithExpr[Q] {

  /**
   * Performs a `JOIN`/`INNER JOIN` on the given [[other]], typically a [[Table]] or [[Select]].
   */
  def join[Q2, R2, QF, RF](other: Joinable[Q2, R2])(on: (Q, Q2) => Sql[Boolean])(
      implicit ja: JoinAppend[Q, Q2, QF, RF]
  ): C[QF, RF] = join0("JOIN", other, Some(on))

  /**
   * Performs a `CROSS JOIN`, which is an `INNER JOIN` but without the `ON` clause
   */
  def crossJoin[Q2, R2, QF, RF](other: Joinable[Q2, R2])(
      implicit ja: JoinAppend[Q, Q2, QF, RF]
  ): C[QF, RF] = join0("CROSS JOIN", other, None)

  protected def join0[Q2, R2, QF, RF](
      prefix: String,
      other: Joinable[Q2, R2],
      on: Option[(Q, Q2) => Sql[Boolean]]
  )(
      implicit ja: JoinAppend[Q, Q2, QF, RF]
  ): C[QF, RF]

  protected def joinInfo[Q2, R2](
      joinPrefix: String,
      other: Joinable[Q2, R2],
      on: Option[(Q, Q2) => Sql[Boolean]]
  ) = {
    val otherSelect = Joinable.toSelect(other)

    val isTrivialJoin = Joinable.isTrivial(other)
    val from =
      if (isTrivialJoin) otherSelect.asInstanceOf[SimpleSelect[_, _]].from.head
      else new SubqueryRef(otherSelect, otherSelect.qr)

    val on2 = on.map(_(expr, otherSelect.expr))
    val otherJoin = Join(joinPrefix, Seq(Join.From(from, on2)))

    (Seq(otherJoin), otherSelect)
  }

}

object JoinOps {

  def join0[C[_, _], Q, R, Q2, R2, QF, RF](
      v: JoinOps[C, Q, R],
      prefix: String,
      other: Joinable[Q2, R2],
      on: Option[(Q, Q2) => Sql[Boolean]]
  )(
      implicit ja: JoinAppend[Q, Q2, QF, RF]
  ) = {
    v.join0[Q2, R2, QF, RF](prefix, other, on)
  }
}
