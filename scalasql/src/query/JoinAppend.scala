package scalasql.query

import scalasql.Queryable

trait JoinAppend[Q, R, Q2, R2, QF, RF]{
  def appendQ(t: Q, v: Q2): QF
  def appendR(t: R, v: R2): RF

  def qr: Queryable.Row[QF, RF]
  def qr2: Queryable.Row[Q2, _]
}
object JoinAppend extends scalasql.generated.JoinAppend{
}
trait JoinAppendLowPriority{
  implicit def default[Q, R, Q2, R2](implicit qr0: Queryable.Row[Q, R],
                                     qr20: Queryable.Row[Q2, R2]) = new JoinAppend[Q, R, Q2, R2, (Q, Q2), (R, R2)]{
    override def appendQ(t: Q, v: Q2): (Q, Q2) = (t, v)
    override def appendR(t: R, v: R2): (R, R2) = (t, v)

    def qr: Queryable.Row[(Q, Q2), (R, R2)] = Queryable.Row.Tuple2Queryable
    def qr2: Queryable.Row[Q2, _] = qr20
  }
}
