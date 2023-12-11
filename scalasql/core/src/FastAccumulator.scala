package scalasql.core

import scala.reflect.ClassTag

class FastAccumulator[T: ClassTag](startSize: Int = 16) {
  val arr = new Array[T](startSize)
}
