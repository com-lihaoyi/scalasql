package scalasql.utils

import scalasql.MappedType
import upickle.core.{ArrVisitor, ObjVisitor, Visitor}
import upickle.core.compat.Factory

object OptionPickler
    extends upickle.core.Types
    with upickle.implicits.CaseClassReadWriters
    with upickle.implicits.Generated
    with upickle.implicits.MacroImplicits {

  implicit def reader[T: MappedType]: SimpleReader[T] = new SimpleReader[T] {
    override def expectedMsg: String = ???
  }
  override def taggedExpectedMsg: String = ???

  override def taggedWrite[T, R](
      w: OptionPickler.ObjectWriter[T],
      tag: String,
      out: Visitor[_, R],
      v: T
  ): R = ???

  implicit def SeqLikeReader2[C[_], T](
      implicit r: Reader[T],
      factory: Factory[T, C[T]]
  ): SeqLikeReader2[C, T] = new SeqLikeReader2[C, T]()

  class SeqLikeReader2[C[_], T](implicit val r: Reader[T], factory: Factory[T, C[T]])
      extends SimpleReader[C[T]] {
    override def expectedMsg = "expected sequence"

    override def visitArray(length: Int, index: Int) = new ArrVisitor[Any, C[T]] {
      val b = factory.newBuilder

      def visitValue(v: Any, index: Int): Unit = b += v.asInstanceOf[T]

      def visitEnd(index: Int) = b.result()

      def subVisitor = r
    }
  }

  trait NullableVisitor
  class NullableArrVisitor[-K, +T](v0: ArrVisitor[K, T])
      extends ArrVisitor[K, T]
      with NullableVisitor {
    def subVisitor: Visitor[_, _] = new NullableReader(v0.subVisitor.asInstanceOf[Reader[_]])
    def visitValue(v: K, index: Int) = v0.visitValue(v, index)
    def visitEnd(index: Int) = v0.visitEnd(index)
  }

  class NullableObjVisitor[-K, +T](v0: ObjVisitor[K, T])
      extends ObjVisitor[K, T]
      with NullableVisitor {
    def subVisitor: Visitor[_, _] = new NullableReader(v0.subVisitor.asInstanceOf[Reader[_]])
    def visitValue(v: K, index: Int) = v0.visitValue(v, index)
    def visitEnd(index: Int) = v0.visitEnd(index)
    def visitKey(index: Int): Visitor[_, _] = v0.visitKey(index)
    def visitKeyValue(v: Any) = v0.visitKeyValue(v)
  }

  class NullableReader[T](reader: Reader[T]) extends Reader.Delegate(reader) with NullableVisitor {
    override def visitArray(length: Int, index: Int): ArrVisitor[Any, T] = {
      new NullableArrVisitor(super.visitArray(length, index))
    }

    override def visitObject(length: Int, jsonableKeys: Boolean, index: Int): ObjVisitor[Any, T] = {
      new NullableObjVisitor(super.visitObject(length, jsonableKeys, index))
    }
  }

  def annotate[V]
    (rw: scalasql.utils.OptionPickler.Reader[V], n: String):
      scalasql.utils.OptionPickler.TaggedReader[V] = ???
    def annotate[V]
    (rw: scalasql.utils.OptionPickler.ObjectWriter[V], n: String, checker:
      upickle.core.Annotator.Checker):
      scalasql.utils.OptionPickler.TaggedWriter[V] = ???
}
