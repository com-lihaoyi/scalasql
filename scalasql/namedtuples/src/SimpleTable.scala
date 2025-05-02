package scalasql.namedtuples

import scalasql.core.DbApi.Impl
import scalasql.core.DialectTypeMappers
import scalasql.core.Expr
import scalasql.core.Queryable
import scalasql.core.Queryable.ResultSetIterator
import scalasql.core.Sc
import scalasql.dialects.Dialect
import scalasql.dialects.TableOps
import scalasql.namedtuples.SimpleTable.Internal.Tombstone
import scalasql.query.Column
import scalasql.query.Query
import scalasql.query.Table
import scalasql.query.Table.ImplicitMetadata
import sourcecode.Name

import scala.NamedTuple.AnyNamedTuple
import scala.language.implicitConversions

class SimpleTable[C]()(
    using val name0: sourcecode.Name,
    metadata0: => SimpleTable.Metadata[C]
) extends Table.Base
    with SimpleTable.LowPri[C] {

  lazy val metadata: SimpleTable.Metadata[C] = metadata0

  override protected[scalasql] def tableName: String = name0.value

  override protected[scalasql] def schemaName: String = ""

  override protected[scalasql] def tableLabels: Seq[String] = {
    metadata.metadata0.walkLabels0()
  }

  override protected[scalasql] def escape: Boolean = false

  given simpleTableImplicitMetadata: SimpleTable.WrappedMetadata[C] =
    SimpleTable.WrappedMetadata(metadata)

  implicit def containerQr(
      implicit dialect: DialectTypeMappers,
      f: SimpleTableMacros.Mask[C]
  ): Queryable.Row[f.Result[Expr], C] =
    val tableMetadata = metadata.metadata0
    tableMetadata
      .queryable(
        tableMetadata.walkLabels0,
        dialect,
        new Table.Metadata.QueryableProxy(tableMetadata.queryables(dialect, _))
      )
      .asInstanceOf[Queryable.Row[f.Result[Expr], C]]
}

object SimpleTable {

  trait LowPri[C] { this: SimpleTable[C] =>
    implicit def containerQr2(
        implicit dialect: DialectTypeMappers,
        f: SimpleTableMacros.Mask[C]
    ): Queryable.Row[f.Result[Column], C] =
      containerQr.asInstanceOf[Queryable.Row[f.Result[Column], C]]
  }

  implicit def TableOpsConv[C: {SimpleTableMacros.Mask as f}](
      t: SimpleTable[C]
  )(using dialect: Dialect): TableOps[f.Result] =
    // assume types in f.Result matches
    val tableMetadata = t.metadata.metadata0.asInstanceOf[Table.Metadata[f.Result]]
    dialect.TableOpsConv(new Table[f.Result](using t.name0, tableMetadata) {
      override protected[scalasql] def tableName: String = t.tableName

      override protected[scalasql] def schemaName: String = t.schemaName

      override protected[scalasql] def tableLabels: Seq[String] = t.tableLabels

      override protected[scalasql] def escape: Boolean = t.escape
    })

  final class Record[C, Mask <: AnyNamedTuple](data: IArray[AnyRef]) extends Selectable:
    type Fields = Mask
    def recordIterator: Iterator[Any] = data.iterator.asInstanceOf[Iterator[Any]]
    def apply(i: Int): AnyRef = data(i)
    def updates(fs: ((u: RecordUpdater[C, Mask]) => u.Patch)*): Record[C, Mask] =
      val u = recordUpdater[C, Mask]
      val arr = IArray.genericWrapArray(data).toArray
      fs.foreach: f =>
        val patch = f(u)
        val idx = patch.idx
        arr(idx) = patch.f(arr(idx))
      Record(IArray.unsafeFromArray(arr))

    inline def selectDynamic(name: String): AnyRef =
      apply(compiletime.constValue[Record.IndexOf[name.type, Record.Names[C], 0]])

  private object RecordUpdaterImpl extends RecordUpdater[Any, AnyNamedTuple]
  def recordUpdater[C, Mask <: AnyNamedTuple]: RecordUpdater[C, Mask] =
    RecordUpdaterImpl.asInstanceOf[RecordUpdater[C, Mask]]
  sealed trait RecordUpdater[C, Mask <: AnyNamedTuple] extends Selectable:
    final case class Patch(idx: Int, f: AnyRef => AnyRef)
    type Fields = NamedTuple.Map[
      Mask,
      [X] =>> (X => X) => Patch
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
    def fromIArray(data: IArray[AnyRef]): Record[Any, AnyNamedTuple] =
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
  class Metadata[C](val f: SimpleTableMacros.Mask[C])(
      val metadata0: Table.Metadata[f.Result]
  ):
    def rowExpr(
        mappers: DialectTypeMappers
    ): Queryable.Row[f.Result[Expr], C] =
      metadata0
        .queryable(
          metadata0.walkLabels0,
          mappers,
          new Table.Metadata.QueryableProxy(metadata0.queryables(mappers, _))
        )
        .asInstanceOf[Queryable.Row[f.Result[Expr], C]]

  object Metadata extends SimpleTableMacros
}
