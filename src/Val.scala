package usql
import upickle.default.Reader

class Val[T](value: T){
  def apply() = value

  override def hashCode(): Int = value.##

  override def equals(obj: Any): Boolean = obj match{
    case v: Val[_] => v() == apply()
    case _ => false
  }

  override def toString: String = s"Val(${value.toString})"
}

object Val{
  implicit def apply[T](value: T): Val[T] = new Val(value)
  implicit def reader[T: Reader]: Reader[Val[T]] = upickle.default.reader[T].map(Val(_))
}

