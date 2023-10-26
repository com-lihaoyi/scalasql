package scalasql.utils

import scalasql.{MappedType, Val}
import upickle.core.{ArrVisitor, Visitor}
import upickle.core.compat.Factory


object OptionPickler extends upickle.core.Types
  with upickle.implicits.CaseClassReadWriters
  with upickle.implicits.Generated
  with upickle.implicits.MacroImplicits {

  implicit def reader[T: MappedType] = new SimpleReader[T] {
    override def expectedMsg: String = ???
  }
  implicit def readerVal[T: MappedType] = new SimpleReader[Val[T]] {
    override def expectedMsg: String = ???
  }
  override def taggedExpectedMsg: String = ???

  override def taggedWrite[T, R](w: OptionPickler.ObjectWriter[T], tag: String, out: Visitor[_, R], v: T): R = ???

  implicit def SeqLikeReader[C[_], T](implicit r: Reader[T],
                                      factory: Factory[T, C[T]]): Reader[C[T]] = new SeqLikeReader[C, T]()

  class SeqLikeReader[C[_], T](implicit r: Reader[T],
                               factory: Factory[T, C[T]]) extends SimpleReader[C[T]] {
    override def expectedMsg = "expected sequence"

    override def visitArray(length: Int, index: Int) = new ArrVisitor[Any, C[T]] {
      val b = factory.newBuilder

      def visitValue(v: Any, index: Int): Unit = b += v.asInstanceOf[T]

      def visitEnd(index: Int) = b.result()

      def subVisitor = r
    }
  }
}
