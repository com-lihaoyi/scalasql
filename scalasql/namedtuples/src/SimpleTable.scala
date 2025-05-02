package scalasql.namedtuples

import scalasql.query.Table
import scalasql.core.DialectTypeMappers
import scalasql.core.Queryable
import scalasql.query.Column
import scalasql.core.Sc
import scalasql.core.Expr

class SimpleTable[C <: SimpleTable.Source]()(
    using name: sourcecode.Name,
    metadata0: SimpleTable.Metadata[C]
) extends Table[SimpleTable.Lift[C]](using name, metadata0.metadata0) {
  given simpleTableImplicitMetadata: SimpleTable.WrappedMetadata[C] =
    SimpleTable.WrappedMetadata(metadata0)
}

object SimpleTable {

  /**
   * Marker class that signals that a data type is convertable to an SQL table row.
   * @note this must be a class to convince the match type reducer that it provably can't be mixed
   *  into various column types such as java.util.Date, geny.Bytes, or scala.Option.
   */
  abstract class Source

  type Lift[C] = [T[_]] =>> T[Internal.Tombstone.type] match {
    case Expr[?] => Record[C, T]
    case _ => C
  }

  final class Record[C, T[_]](data: IArray[AnyRef]) extends Selectable:
    type Fields = NamedTuple.Map[
      NamedTuple.From[C],
      [X] =>> X match {
        case Source => Record[X, T]
        case _ => T[X]
      }
    ]
    def recordIterator: Iterator[Any] = data.iterator.asInstanceOf[Iterator[Any]]
    def apply(i: Int): AnyRef = data(i)
    inline def selectDynamic(name: String): AnyRef =
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
  class Metadata[C](val metadata0: Table.Metadata[Lift[C]]):
    def rowExpr(
        mappers: DialectTypeMappers
    ): Queryable.Row[Record[C, Expr], C] =
      metadata0
        .queryable(
          metadata0.walkLabels0,
          mappers,
          new Table.Metadata.QueryableProxy(metadata0.queryables(mappers, _))
        )
        .asInstanceOf[Queryable.Row[Record[C, Expr], C]]

  object Metadata extends SimpleTableMacros
}
