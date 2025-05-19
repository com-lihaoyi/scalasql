package scalasql.namedtuples

import scala.NamedTuple.{AnyNamedTuple, NamedTuple}

import scalasql.query.Table
import scalasql.core.DialectTypeMappers
import scalasql.core.Queryable
import scalasql.query.Column
import scalasql.core.Sc
import scalasql.core.Expr

class SimpleTable[C]()(
    using name: sourcecode.Name,
    metadata0: SimpleTable.Metadata[C]
) extends Table[[T[_]] =>> SimpleTable.MapOver[C, T]](using name, metadata0.metadata0) {
  given simpleTableImplicitMetadata: SimpleTable.WrappedMetadata[C] =
    SimpleTable.WrappedMetadata(metadata0)
}

object SimpleTable {

  /**
   * Marker class that signals that a data type is convertable to an SQL table row.
   * @note this must be a class to convince the match type reducer that it provably can't be mixed
   *  into various column types such as java.util.Date, geny.Bytes, or scala.Option.
   */
  abstract class Nested

  /**
   * A type that can map `T` over the fields of `C`. If `T` is the identity then `C` itself,
   * else `Record[C, T]`
   */
  type MapOver[C, T[_]] = T[Internal.Tombstone.type] match {
    case Internal.Tombstone.type => C // T is `Sc`
    case _ => Record[C, T]
  }

  final class Record[C, T[_]](data: IArray[AnyRef]) extends Selectable:
    type Fields = NamedTuple.Map[
      NamedTuple.From[C],
      [X] =>> X match {
        case Nested => Record[X, T]
        case _ => T[X]
      }
    ]
    def recordIterator: Iterator[Any] = data.iterator.asInstanceOf[Iterator[Any]]
    def apply(i: Int): AnyRef = data(i)
    def updates(fs: ((u: RecordUpdater[C, T]) => u.Patch)*): Record[C, T] =
      val u = recordUpdater[C, T]
      val arr = IArray.genericWrapArray(data).toArray
      fs.foreach: f =>
        val patch = f(u)
        val idx = patch.idx
        arr(idx) = patch.f(arr(idx))
      Record(IArray.unsafeFromArray(arr))

    inline def selectDynamic(name: String): AnyRef =
      apply(compiletime.constValue[Record.IndexOf[name.type, Record.Names[C], 0]])

  private object RecordUpdaterImpl extends RecordUpdater[Any, [T] =>> Any]
  def recordUpdater[C, T[_]]: RecordUpdater[C, T] =
    RecordUpdaterImpl.asInstanceOf[RecordUpdater[C, T]]
  sealed trait RecordUpdater[C, T[_]] extends Selectable:
    final case class Patch(idx: Int, f: AnyRef => AnyRef)
    type Fields = NamedTuple.Map[
      NamedTuple.From[C],
      [X] =>> X match {
        case Nested => (Record[X, T] => Record[X, T]) => Patch
        case _ => (T[X] => T[X]) => Patch
      }
    ]
    def apply(i: Int): (AnyRef => AnyRef) => Patch =
      f => Patch(i, f)
    inline def selectDynamic(name: String): (AnyRef => AnyRef) => Patch =
      apply(compiletime.constValue[Record.IndexOf[name.type, Record.Names[C], 0]])

  object Record:
    import scala.compiletime.ops.int.*
    type Names[C] = NamedTuple.Names[NamedTuple.From[C]]
    type IndexOf[N, T <: Tuple, Acc <: Int] <: Int = T match {
      case EmptyTuple => -1
      case N *: _ => Acc
      case _ *: t => IndexOf[N, t, S[Acc]]
    }
    def fromIArray(data: IArray[AnyRef]): Record[Any, [T] =>> Any] =
      Record(data)

  object Internal {
    case object Tombstone
  }

  opaque type WrappedMetadata[C] = Metadata[C]
  object WrappedMetadata {
    def apply[C](metadata: Metadata[C]): WrappedMetadata[C] = metadata
    extension [C](m: WrappedMetadata[C]) {
      def metadata: Metadata[C] = m
    }
  }
  class Metadata[C](val metadata0: Table.Metadata[[T[_]] =>> SimpleTable.MapOver[C, T]])

  object Metadata extends SimpleTableMacros
}
