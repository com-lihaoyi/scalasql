package scalasql.query

import scalasql.core.Queryable

/**
 * Typeclass to allow `.join` to append tuples, such that `Query[(A, B)].join(Query[C])`
 * returns a flat `Query[(A, B, C)]` rather than a nested `Query[((A, B), B)]`. Can't
 * eliminate nesting in all cases, but eliminates nesting often enough to be useful
 */
trait JoinAppend[Q, Q2, QF, RF] {
  def appendTuple(t: Q, v: Q2): QF

  def qr: Queryable.Row[QF, RF]
}
object JoinAppend extends scalasql.generated.JoinAppend {}
trait JoinAppendLowPriority {
  implicit def default[Q, R, Q2, R2](
      implicit qr0: Queryable.Row[Q, R],
      qr20: Queryable.Row[Q2, R2]
  ): JoinAppend[Q, Q2, (Q, Q2), (R, R2)] = new JoinAppend[Q, Q2, (Q, Q2), (R, R2)] {
    override def appendTuple(t: Q, v: Q2): (Q, Q2) = (t, v)

    def qr: Queryable.Row[(Q, Q2), (R, R2)] = Queryable.Row.Tuple2Queryable
  }
}
