package scalasql.namedtuples

import scalasql.query.Table0
import scalasql.query.Table.Internal.TableQueryable
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
import java.util.function.UnaryOperator
import scala.annotation.nowarn
import scalasql.namedtuples.SimpleTableMacros.BaseLabels
import scalasql.namedtuples.SimpleTable.{Record, Columns}
import scalasql.core.TypeMapper
import scala.annotation.unused
import scala.annotation.implicitNotFound
import scalasql.namedtuples.SimpleTable.AnyRecord

object SimpleTableMacros {
  def asIArray[T: ClassTag](t: Tuple): IArray[T] = {
    IArray.from(t.productIterator.asInstanceOf[Iterator[T]])
  }
  def asIArrayFlatUnwrapWithIndex[T](
      t: Tuple
  )[U: ClassTag](f: (T, Int) => IterableOnce[U]): IArray[U] = {
    IArray.from(t.productIterator.asInstanceOf[Iterator[T]].zipWithIndex.flatMap(f.tupled))
  }
  def unwrapLabels(t: Tuple, labels: IArray[String]): IndexedSeq[String] = {
    asIArrayFlatUnwrapWithIndex[BaseLabels[Any, Any]](t)((l, i) => l(labels(i))).toIndexedSeq
  }
  def unwrapColumns(t: Tuple): IArray[(DialectTypeMappers, TableRef) => AnyRef] = {
    asIArray[ContraRefMapper[BaseColumn[Any, Any, [_] =>> Any]]](t)
  }
  def unwrapRows(t: Tuple): IArray[DialectTypeMappers => Queryable.Row[?, ?]] = {
    asIArray[ContraMapper[SimpleTableMacros.BaseRowExpr[Any, [_] =>> Any, [_] =>> Any]]](t)
  }
  def make[C](m: Mirror.ProductOf[C], data: IArray[AnyRef]): C = {
    class ArrayProduct extends Product {
      override def canEqual(that: Any): Boolean = false
      override def productElement(n: Int): Any = data(n)
      override def productIterator: Iterator[Any] = data.iterator
      override def productPrefix: String = "ArrayProduct"
      override def productArity: Int = data.length
    }
    m.fromProduct(ArrayProduct())
  }

  opaque type ContraMapper[T] = DialectTypeMappers => T

  object ContraMapper {
    inline given [T]: ContraMapper[T] =
      case mappers =>
        import mappers.given
        compiletime.summonInline[T]
  }

  opaque type ContraRefMapper[T] = (DialectTypeMappers, TableRef) => T

  object ContraRefMapper {
    @nowarn("msg=inline given alias")
    inline given [T]: ContraRefMapper[T] =
      (mappers, tableRef) =>
        tableRef match
          case given TableRef =>
            import mappers.given
            compiletime.summonInline[T]
  }

  opaque type BaseRowExpr[C, T[_], Self[_]] = Queryable.Row[?, ?]
  object BaseRowExpr {
    given summonDelegate[C, T[_], Self[_]](
        using @unused m: SimpleTable.GivenMetadata[C],
        @unused e: C <:< SimpleTable.Nested
    )(
        using delegate: Queryable.Row[Self[C], C]
    ): BaseRowExpr[C, T, Self] = delegate
    given summonBasic[C, T[_], Self[_]](
        using @unused ev: scala.util.NotGiven[SimpleTable.GivenMetadata[C]]
    )(
        using delegate: Queryable.Row[T[C], C]
    ): BaseRowExpr[C, T, Self] = delegate
  }

  opaque type BaseColumn[L, T, Self[_]] = AnyRef
  trait BaseColumnLowPrio {
    given notFound: [L <: String, T, Self[_]]
      => (l: ValueOf[L], ref: TableRef, mapper: TypeMapper[T])
      => BaseColumn[L, T, Self] =
      val col = new Column[T](
        ref,
        Table0.columnNameOverride(ref.value)(l.value)
      )
      col
  }
  object BaseColumn extends BaseColumnLowPrio {
    given foundMetaCol: [L <: String, T]
      => (mappers: DialectTypeMappers, ref: TableRef, m: SimpleTable.GivenMetadata[T])
      => BaseColumn[L, T, Columns] =
      m.metadata.vCol(ref, mappers).asInstanceOf[AnyRef]
    given foundMeta: [L <: String, T]
      => (mappers: DialectTypeMappers, ref: TableRef, m: SimpleTable.GivenMetadata[T])
      => BaseColumn[L, T, Record] =
      m.metadata.vStrictExpr(ref, mappers).asInstanceOf[AnyRef]
  }

  opaque type BaseLabels[L, C] = String => Seq[String]
  trait BaseLabelsLowPrio {
    given notFound: [L <: String, C] => BaseLabels[L, C] =
      label => Seq(label)
  }
  object BaseLabels extends BaseLabelsLowPrio {
    given foundMeta: [L, C] => (m: SimpleTable.GivenMetadata[C]) => BaseLabels[L, C] =
      _ => m.metadata.walkLabels0()
  }

  def setNonNull[T](r: AtomicReference[T | Null])(f: => T): T = {
    val local = r.get()
    val res =
      if local != null then local
      else
        r.updateAndGet(t =>
          if t == null then f
          else t
        )
    res.nn
  }

  def walkAllExprs(
      queryable: Table0.Metadata.QueryableProxy
  )(e: SimpleTable.AnyRecord): IndexedSeq[Expr[?]] = {
    var i = 0
    val fields = e.productIterator
    val buf = IndexedSeq.newBuilder[Seq[Expr[?]]]
    while fields.hasNext do
      type T
      type Field
      val field = fields.next().asInstanceOf[Field]
      val row = queryable[Field, T](i)
      buf += row.walkExprs(field)
      i += 1
    buf.result().flatten
  }

  def construct[C](
      queryable: Table0.Metadata.QueryableProxy
  )(size: Int, args: Queryable.ResultSetIterator, factory: IArray[AnyRef] => C): C = {
    var i = 0
    val buf = IArray.newBuilder[AnyRef]
    while i < size do
      type T
      type Field
      val row = queryable[Field, T](i)
      buf += row.construct(args).asInstanceOf[AnyRef]
      i += 1
    factory(buf.result())
  }

  def deconstruct(
      queryable: Table0.Metadata.QueryableProxy
  )(c: Product): IArray[AnyRef] = {
    var i = 0
    val buf = IArray.newBuilder[AnyRef]
    val fields = c.productIterator
    while fields.hasNext do
      type T
      type Field
      val field = fields.next().asInstanceOf[T]
      val row = queryable[Field, T](i)
      buf += row.deconstruct(field).asInstanceOf[AnyRef]
      i += 1
    buf.result()
  }

  def labels(t: Tuple): IArray[String] = asIArray(t)

  inline def getMirror[C]: (Tuple, Mirror.ProductOf[C]) = {
    compiletime.summonFrom { case m: Mirror.ProductOf[C] =>
      (compiletime.constValueTuple[m.MirroredElemLabels], m)
    }
  }

}

trait SimpleTableMacros {
  class SimpleTableState[C <: Product](
      mirrorPair0: => (Tuple, Mirror.ProductOf[C]),
      colRowsRef0: => Tuple,
      colsRef0: => Tuple,
      labelsRef0: => Tuple
  ):
    private lazy val mirrorPair =
      val (names0, mirror0) = mirrorPair0
      (
        SimpleTableMacros.labels(names0),
        mirror0
      )
    def labels: IArray[String] = mirrorPair(0)
    def mirror: Mirror.ProductOf[C] = mirrorPair(1)
    lazy val colRowsRef: IArray[DialectTypeMappers => Queryable.Row[?, ?]] =
      SimpleTableMacros.unwrapRows(colRowsRef0)
    lazy val colsRef: IArray[(DialectTypeMappers, TableRef) => AnyRef] =
      SimpleTableMacros.unwrapColumns(colsRef0)
    lazy val labelsRef: IndexedSeq[String] =
      SimpleTableMacros.unwrapLabels(labelsRef0, labels)

  inline given initTableMetadata[C <: Product]: Table0.SharedMetadata[Record[C], Columns[C], C] =
    type Labels = NamedTuple.Names[NamedTuple.From[C]]
    type Values = NamedTuple.DropNames[NamedTuple.From[C]]
    type Pairs[F[_, _]] = Tuple.Map[
      Tuple.Zip[Labels, Values],
      [X] =>> X match {
        case (a, b) => F[a, b]
      }
    ]
    type FlatLabels = Pairs[SimpleTableMacros.BaseLabels]
    type ColumnsOf = Pairs[[L,
    T] =>> SimpleTableMacros.ContraRefMapper[SimpleTableMacros.BaseColumn[L, T, Columns]]]
    type ColExprsOf = Tuple.Map[
      Values,
      [T] =>> SimpleTableMacros.ContraMapper[SimpleTableMacros.BaseRowExpr[T, Column, Columns]]
    ]

    val state = new SimpleTableState[C](
      mirrorPair0 = SimpleTableMacros.getMirror[C],
      colRowsRef0 = compiletime.summonAll[ColExprsOf],
      colsRef0 = compiletime.summonAll[ColumnsOf],
      labelsRef0 = compiletime.summonAll[FlatLabels]
    )

    def queryables(mappers: DialectTypeMappers, idx: Int): Queryable.Row[?, ?] =
      state.colRowsRef(idx)(mappers)
    def walkLabels0(): Seq[String] = state.labelsRef

    def queryable(
        walkLabels0: () => Seq[String],
        @nowarn("msg=unused") mappers: DialectTypeMappers,
        queryable: Table0.Metadata.QueryableProxy
    ): Queryable[Record[C], C] = TableQueryable(
      walkLabels0,
      walkExprs0 = SimpleTableMacros.walkAllExprs(queryable),
      construct0 = args =>
        SimpleTableMacros.construct(queryable)(
          size = state.labels.size,
          args = args,
          factory = SimpleTableMacros.make(state.mirror, _)
        ),
      deconstruct0 = values =>
        // columns can downcast to Record[C]
        Columns[C](SimpleTableMacros.deconstruct(queryable)(values))
    )

    def vCol0(
        tableRef: TableRef,
        mappers: DialectTypeMappers,
        @nowarn("msg=unused") queryable: Table0.Metadata.QueryableProxy
    ): Columns[C] =
      val columns = state.colsRef.map(_(mappers, tableRef))
      Columns[C](columns)

    Table0.SharedMetadata[Record[C], Columns[C], C](
      queryables,
      walkLabels0,
      queryable,
      vCol0
    )
}
