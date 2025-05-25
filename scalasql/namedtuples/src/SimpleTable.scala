package scalasql.namedtuples

import scala.NamedTuple.{AnyNamedTuple, NamedTuple}

import scalasql.query.Table0
import scalasql.core.DialectTypeMappers
import scalasql.core.Queryable
import scalasql.query.Column
import scalasql.core.Sc
import scalasql.core.Expr

import scalasql.namedtuples.SimpleTable.{Record, Columns}

import scala.compiletime.asMatchable
import scala.reflect.ClassTag

/**
 * In-code representation of a SQL table, associated with a given `case class` `C`.
 *
 * Note that if a field of `C` is a case class `X` that also provides SimpleTable metadata,
 * then `X` must extend [[package.SimpleTable.Nested SimpleTable.Nested]].
 *
 * `SimpleTable` extends `Table`, sharing its underlying metadata.
 * Compared to `Table`, it allows to `C` to not require a higher-kinded type parameter.
 * Consequently a [[package.SimpleTable.Record Record]] is used in queries
 * rather than `C` itself.
 */
class SimpleTable[C](
    using name: sourcecode.Name,
    metadata0: Table0.Metadata[Record[C], Columns[C], C]
) extends Table0[Record[C], Columns[C], C](using name, metadata0) {
  given simpleTableGivenMetadata: SimpleTable.GivenMetadata[C] =
    SimpleTable.GivenMetadata(metadata0)
}

object SimpleTable extends SimpleTableMacros {

  /**
   * Marker class that signals that a data type is convertable to an SQL table row.
   * @note this must be a class to convince the match type reducer that it provably can't be mixed
   *  into various column types such as `java.util.Date`, `geny.Bytes`, or `scala.Option`.
   */
  abstract class Nested

  /** Super type of all [[SimpleTable.Record Record]]. */
  sealed trait AnyRecord extends Product with Serializable

  final class Record[C](data: IArray[AnyRef]) extends BaseRecord[C](data) {
    type T[X] = Expr[X]
    type Self[X] = Record[X]

    def canEqual(that: Any): Boolean = that.isInstanceOf[Record[?]]
    protected def factory(arr: IArray[AnyRef]): Self[C] = Record(arr)
  }
  final class Columns[C](data: IArray[AnyRef]) extends BaseRecord[C](data) {
    override type T[X] = Column[X]
    override type Self[X] = Columns[X]

    def canEqual(that: Any): Boolean = that.isInstanceOf[Columns[?]]
    protected def factory(arr: IArray[AnyRef]): Self[C] = Columns(arr)
  }

  /**
   * Record is a fixed size product type, its fields correspond to the fields of `C`
   * mapped over by `T` (see [[Record#Fields Fields]] for more information).
   *
   * @see [[Record#Fields Fields]] for how the fields are mapped.
   */
  sealed abstract class BaseRecord[C](private val data: IArray[AnyRef]) extends AnyRecord with Selectable:
    type T[_]
    type Self[_]

    /**
     * For each field `x: X` of class `C` there exists a field `x` in this record of type
     * `Record[X, T]` if `X` is a case class that represents a table, or `T[X]` otherwise.
     */
    type Fields = NamedTuple.Map[
      NamedTuple.From[C],
      [X] =>> X match {
        case Nested => Self[X]
        case _ => T[X]
      }
    ]
    def apply(i: Int): AnyRef = data(i)
    override def productPrefix: String = "Record"
    def productArity: Int = data.length
    def productElement(i: Int): AnyRef = data(i)
    override def equals(that: Any): Boolean = that.asMatchable match
      case _: this.type => true
      case r: BaseRecord[?] =>
        r.canEqual(this) && IArray.equals(data, r.data)
      case _ => false

    /**
     * Apply a sequence of patches to the record. e.g.
     * ```
     * case class Foo(arg1: Int, arg2: String)
     * val r: Record[Foo, Expr]
     * val r0 = r.updates(_.arg1(_ * 2), _.arg2 := "bar")
     * ```
     *
     * @param fs a sequence of functions that create a patch from a [[RecordUpdater]].
     *   Each field of the record updater is typed as a [[SimpleTable.Field Field]],
     *   corresponding to the fields of `C` mapped over by `T`.
     *   in this record.
     * @return a new record (of the same type) with the patches applied.
     */
    def updates(fs: (RecordUpdater[C, T, Self] => Patch)*): Self[C] =
      val u = recordUpdater[C, T, Self]
      val arr = IArray.genericWrapArray(data).toArray
      fs.foreach: f =>
        val patch = f(u)
        val idx = patch.idx
        arr(idx) = patch.f(arr(idx))
      factory(IArray.unsafeFromArray(arr))

    protected def factory(arr: IArray[AnyRef]): Self[C]

    inline def selectDynamic(name: String): AnyRef =
      apply(compiletime.constValue[Record.IndexOf[name.type, Record.Names[C], 0]])

  private object RecordUpdaterImpl extends RecordUpdater[Any, ?, ?]
  def recordUpdater[C, T[_], Self[_]]: RecordUpdater[C, T, Self] =
    RecordUpdaterImpl.asInstanceOf[RecordUpdater[C, T, Self]]

  /** A single update to a field of a `Record[C, T]`, used by [[Record#updates]] */
  final class Patch private[SimpleTable] (
      private[SimpleTable] val idx: Int,
      private[SimpleTable] val f: AnyRef => AnyRef
  )

  /** A `Field[T]` is used to create a patch for a field in a [[SimpleTable.Record Record]]. */
  final class Field[T](private val factory: (T => T) => Patch) extends AnyVal
  object Field {
    extension [T](field: Field[T]) {

      /** Create a patch that replaces the old value with `x` */
      def :=(x: T): Patch = field.factory(Function.const(x))

      /** Create a patch that can transform the old value with `f` */
      def apply(f: T => T): Patch = field.factory(f)
    }
  }

  /**
   * A Record updaters fields correspond to `Record[C, T]`, where each accepts a
   * function to update a field. e.g.
   * ```
   * case class Foo(arg1: Int, arg2: String)
   * val u: RecordUpdater[Foo, Expr]
   * val p0: Patch = u.arg1(_ * 2)
   * val p1: Patch = u.arg2 := "bar"
   * ```
   * This class is mainly used to provide patches to
   * the [[Record#updates updates]] method of `Record`.
   * (See [[RecordUpdater#Fields Fields]] for more information on how the fields are typed.)
   *
   * @see [[Record#updates updates]] for how to apply the patches.
   * @see [[RecordUpdater#Fields Fields]] for how the fields are mapped.
   */
  sealed trait RecordUpdater[C, T[_], Self[_]] extends Selectable:

    /**
     * For each field `x: X` of class `C`
     * there exists a field `x: Field[X']` in this record updater. `X'` is instantiated to
     * `Record[X, T]` if `X` is a case class that represents a table, or `T[X]` otherwise.
     */
    type Fields = NamedTuple.Map[
      NamedTuple.From[C],
      [X] =>> X match {
        case Nested => Field[Self[X]]
        case _ => Field[T[X]]
      }
    ]
    def apply(i: Int): Field[AnyRef] =
      new Field(f => Patch(i, f))
    inline def selectDynamic(name: String): Field[AnyRef] =
      apply(compiletime.constValue[Record.IndexOf[name.type, Record.Names[C], 0]])

  object Record:
    import scala.compiletime.ops.int.*

    /** Tuple of literal strings corresponding to the fields of case class `C` */
    type Names[C] = NamedTuple.Names[NamedTuple.From[C]]

    /** The literal `Int` type corresponding to the index of `N` in `T`, or `-1` if not found. */
    type IndexOf[N, T <: Tuple, Acc <: Int] <: Int = T match {
      case EmptyTuple => -1
      case N *: _ => Acc
      case _ *: t => IndexOf[N, t, S[Acc]]
    }

  /** Internal API of SimpleTable */
  object Internal {

    /** An object with singleton type that is provably disjoint from most other types. */
    case object Tombstone
  }

  /** A type that gives access to the Table metadata of `C`. */
  opaque type GivenMetadata[C] = GivenMetadata.Inner[C]
  object GivenMetadata {
    type Inner[C] = Table0.Metadata[Record[C], Columns[C], C]
    def apply[C](metadata: Inner[C]): GivenMetadata[C] = metadata
    extension [C](m: GivenMetadata[C]) {
      def metadata: Inner[C] = m
    }
  }

}
