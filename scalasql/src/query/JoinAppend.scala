package scalasql.query

trait JoinAppend[T, V, U]{
  def append(t: T, v: V): U
}
object JoinAppend extends JoinAppendLowPriority with scalasql.generated.JoinAppend{
}
trait JoinAppendLowPriority{
  implicit def default[T, V] = new JoinAppend[T, V, (T, V)]{
    override def append(t: T, v: V): (T, V) = (t, v)
  }
}
