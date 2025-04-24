package scalasql.namedtuples

import scalasql.query.Table
import scalasql.core.Queryable
import scalasql.core.DialectTypeMappers
import scalasql.core.Expr
import scalasql.core.Sc
import scalasql.query.TableRef
import scalasql.query.Column
import scala.deriving.Mirror
import java.util.concurrent.atomic.AtomicReference
import scala.reflect.ClassTag
import scala.NamedTuple.AnyNamedTuple
import scalasql.namedtuples.SimpleTableMacros.asIArray
import java.util.function.UnaryOperator
import scala.annotation.nowarn
import scalasql.namedtuples.SimpleTableMacros.BaseLabels

object SimpleTableMacros {
  // case class State(rows: IArray[Queryable.Row[?, ?]] | Null)

  def asIArray[T: ClassTag](t: Tuple): IArray[T] = {
    IArray.from(t.productIterator.asInstanceOf[Iterator[T]])
  }
  def asIArrayFlatUnwrap[T](t: Tuple)[U: ClassTag](f: T => IterableOnce[U]): IArray[U] = {
    IArray.from(t.productIterator.asInstanceOf[Iterator[T]].flatMap(f))
  }
  def unwrapLabels(t: Tuple): IndexedSeq[String] = {
    asIArrayFlatUnwrap[BaseLabels[?,?]](t)(_.value).toIndexedSeq
  }

  inline def computeRows[V <: Tuple](mappers: DialectTypeMappers): IArray[Queryable.Row[?, ?]] = {
    @nowarn("msg=unused")
    given DialectTypeMappers = mappers
    computeRows0(compiletime.summonAll[Tuple.Map[V, BaseRowExpr]])
  }

  def computeRows0(t: Tuple): IArray[Queryable.Row[?, ?]] = {
    asIArray[BaseRowExpr[?]](t).map(_.value)
  }

  class BaseRowExpr[C](val value: Queryable.Row[?, ?])
  trait BaseRowExprLowPrio {
    inline given notFound: [T] => (mappers: DialectTypeMappers) => BaseRowExpr[T] =
      import mappers.{*, given}
      BaseRowExpr(compiletime.summonInline[Queryable.Row[Expr[T], Sc[T]]])
  }
  object BaseRowExpr extends BaseRowExprLowPrio {
    given foundMeta: [C] => (mappers: DialectTypeMappers, m: SimpleTable.WrappedMetadata[C]) => BaseRowExpr[C] =
      BaseRowExpr(m.metadata.rowExpr(mappers))
  }

  class BaseLabels[L, C](val value: Seq[String])
  trait BaseLabelsLowPrio {
    given notFound: [L <: String, C] => (v: ValueOf[L]) => BaseLabels[L, C] = new BaseLabels(value = List(v.value))
  }
  object BaseLabels extends BaseLabelsLowPrio {
    given foundMeta: [L, C] => (m: SimpleTable.WrappedMetadata[C]) => BaseLabels[L, C] = new BaseLabels(value = m.metadata.metadata0.walkLabels0())
  }

  def setNonNull[T](r: AtomicReference[T | Null])(f: T | Null => T): T = {
    val local = r.get()
    val res =
      if local != null then local
      else r.updateAndGet(f(_))
    res.nn
  }

  def walkAllExprs(queryable: Table.Metadata.QueryableProxy)[E <: Tuple](e: E): IndexedSeq[Expr[?]] = {
    var i = 0
    val fields = e.productIterator
    val buf = IndexedSeq.newBuilder[Seq[Expr[?]]]
    while fields.hasNext do
      type T
      val field = fields.next().asInstanceOf[Expr[T]]
      val row = queryable[Expr[T], Sc[T]](i)
      buf += row.walkExprs(field)
      i += 1
    buf.result().flatten
  }

  def construct[R <: AnyNamedTuple](queryable: Table.Metadata.QueryableProxy)(size: Int, args: Queryable.ResultSetIterator): R = {
    var i = 0
    val buf = IArray.newBuilder[AnyRef]
    while i < size do
      type T
      val row = queryable[Expr[T], Sc[T]](i)
      buf += row.construct(args).asInstanceOf[AnyRef]
      i += 1
    Tuple.fromIArray(buf.result()).asInstanceOf[R]
  }

  def deconstruct[R <: AnyNamedTuple](queryable: Table.Metadata.QueryableProxy)[T <: Tuple](t: T): R = {
    var i = 0
    val buf = IArray.newBuilder[AnyRef]
    val fields = t.productIterator
    while fields.hasNext do
      type T
      val field = fields.next().asInstanceOf[Sc[T]]
      val row = queryable[Expr[T], Sc[T]](i)
      buf += row.deconstruct(field).asInstanceOf[AnyRef]
      i += 1
    Tuple.fromIArray(buf.result()).asInstanceOf[R]
  }

}

trait SimpleTableMacros {
  inline given initTableMetadata[C]: SimpleTable.Metadata[C] =
    // val m = compiletime.summonInline[Mirror.Of[NamedTuple.From[C]]]
    type Impl = SimpleTable.NamedTupleOf[C]
    type Labels = NamedTuple.Names[NamedTuple.From[C]]
    type Values = NamedTuple.DropNames[NamedTuple.From[C]]
    type Size = NamedTuple.Size[NamedTuple.From[C]]
    type FlatLabels = Tuple.Map[Tuple.Zip[Labels,Values], [X] =>> X match {case(l, c)=>SimpleTableMacros.BaseLabels[l, c]}]

    val rowsRef = AtomicReference[IArray[Queryable.Row[?, ?]] | Null](null)
    val labelsRef = AtomicReference[IndexedSeq[String] | Null](null)

    def queryables(mappers: DialectTypeMappers, idx: Int): Queryable.Row[?, ?] =
      SimpleTableMacros.setNonNull(rowsRef)(rows =>
        if rows == null then SimpleTableMacros.computeRows[Values](mappers)
        else rows.nn
      )(idx)

    def walkLabels0(): Seq[String] =
      SimpleTableMacros.setNonNull(labelsRef)(labels =>
        if labels == null then
          SimpleTableMacros.unwrapLabels(compiletime.summonAll[FlatLabels])
        else labels.nn
      )

    def queryable(
        walkLabels0: () => Seq[String],
        @nowarn("msg=unused") mappers: DialectTypeMappers,
        queryable: Table.Metadata.QueryableProxy
    ): Queryable[Impl[Expr], Impl[Sc]] = SimpleTable.Internal.SimpleTableQueryable(
      walkLabels0,
      walkExprs0 = exprs => SimpleTableMacros.walkAllExprs(queryable)(exprs.toTuple),
      construct0 = args =>
        SimpleTableMacros.construct[Impl[Sc]](queryable)(compiletime.constValue[Size], args),
      deconstruct0 = values => SimpleTableMacros.deconstruct[Impl[Expr]](queryable)(values.toTuple)
    )

    def vExpr0(
        tableRef: TableRef,
        mappers: DialectTypeMappers,
        queryable: Table.Metadata.QueryableProxy
    ): Impl[Column] = ???

    val metadata0 = Table.Metadata[Impl](queryables, walkLabels0, queryable, vExpr0)

    SimpleTable.Metadata(metadata0)
}
