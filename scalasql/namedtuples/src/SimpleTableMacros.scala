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
import java.util.function.UnaryOperator
import scala.annotation.nowarn
import scalasql.namedtuples.SimpleTableMacros.BaseLabels
import scalasql.core.TypeMapper

object SimpleTableMacros {
  def asIArray[T: ClassTag](t: Tuple): IArray[T] = {
    IArray.from(t.productIterator.asInstanceOf[Iterator[T]])
  }
  def asIArrayFlatUnwrap[T](t: Tuple)[U: ClassTag](f: T => IterableOnce[U]): IArray[U] = {
    IArray.from(t.productIterator.asInstanceOf[Iterator[T]].flatMap(f))
  }
  def asIArrayUnwrap[T](t: Tuple)[U: ClassTag](f: T => U): IArray[U] = {
    IArray.from(t.productIterator.asInstanceOf[Iterator[T]].map(f))
  }
  def unwrapLabels(t: Tuple): IndexedSeq[String] = {
    asIArrayFlatUnwrap[BaseLabels[?,?]](t)(_.value).toIndexedSeq
  }
  def unwrapColumns(t: Tuple): IArray[AnyRef] = {
    asIArrayUnwrap[BaseColumn[?,?]](t)(_.value)
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

  inline def computeRows[Rows <: Tuple](mappers: DialectTypeMappers): IArray[Queryable.Row[?, ?]] = {
    val rows = mappers match
      case given DialectTypeMappers => compiletime.summonAll[Rows]
    computeRows0(rows)
  }
  inline def computeColumns[Columns <: Tuple](
      mappers: DialectTypeMappers, tableRef: TableRef): IArray[AnyRef] = {
    val cols = mappers match
      case given DialectTypeMappers =>
        tableRef match
          case given TableRef => compiletime.summonAll[Columns]
    unwrapColumns(cols)
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

  class BaseColumn[L, T](val value: AnyRef)
  trait BaseColumnLowPrio {
    inline given notFound: [L <: String, T] => (l: ValueOf[L], mappers: DialectTypeMappers, ref: TableRef) => BaseColumn[L, T] =
      import mappers.{*, given}
      val col = new Column[T](
        ref,
        Table.columnNameOverride(ref.value)(l.value)
      )(using compiletime.summonInline[TypeMapper[T]])
      BaseColumn(col)
  }
  object BaseColumn extends BaseColumnLowPrio {
    given foundMeta: [L <: String, T] => (mappers: DialectTypeMappers, ref: TableRef, m: SimpleTable.WrappedMetadata[T]) => BaseColumn[L, T] =
      BaseColumn(m.metadata.metadata0.vExpr(ref, mappers).asInstanceOf[AnyRef])
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

  def walkAllExprs(queryable: Table.Metadata.QueryableProxy)(e: SimpleTable.Record[?, ?]): IndexedSeq[Expr[?]] = {
    var i = 0
    val fields = e.recordIterator
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

  def construct[C](queryable: Table.Metadata.QueryableProxy)(size: Int, args: Queryable.ResultSetIterator, factory: IArray[AnyRef] => C): C = {
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

  def deconstruct[R <: SimpleTable.Record[?, ?]](queryable: Table.Metadata.QueryableProxy)(c: Product): R = {
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
    SimpleTable.Record.fromIArray(buf.result()).asInstanceOf[R]
  }

}

trait SimpleTableMacros {
  inline given initTableMetadata[C <: Product & SimpleTable.Source]: SimpleTable.Metadata[C] =
    def m = compiletime.summonInline[Mirror.ProductOf[C]]
    type Impl = SimpleTable.Lift[C]
    type Labels = NamedTuple.Names[NamedTuple.From[C]]
    type Values = NamedTuple.DropNames[NamedTuple.From[C]]
    type Size = NamedTuple.Size[NamedTuple.From[C]]
    type Pairs[F[_,_]] = Tuple.Map[Tuple.Zip[Labels,Values], [X] =>> X match {
      case (a, b) => F[a, b]
    }]
    type FlatLabels = Pairs[SimpleTableMacros.BaseLabels]
    type Columns = Pairs[SimpleTableMacros.BaseColumn]
    type Rows = Tuple.Map[Values, SimpleTableMacros.BaseRowExpr]

    val rowsRef = AtomicReference[IArray[Queryable.Row[?, ?]] | Null](null)
    val labelsRef = AtomicReference[IndexedSeq[String] | Null](null)

    def queryables(mappers: DialectTypeMappers, idx: Int): Queryable.Row[?, ?] =
      SimpleTableMacros.setNonNull(rowsRef)(rows =>
        if rows == null then SimpleTableMacros.computeRows[Rows](mappers)
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
    ): Queryable[Impl[Expr], Impl[Sc]] = Table.Internal.TableQueryable(
      walkLabels0,
      walkExprs0 = SimpleTableMacros.walkAllExprs(queryable),
      construct0 = args =>
        SimpleTableMacros.construct(queryable)(
          size = compiletime.constValue[Size],
          args = args,
          factory = SimpleTableMacros.make(m, _)
        ),
      deconstruct0 = values => SimpleTableMacros.deconstruct[Impl[Expr]](queryable)(values)
    )

    def vExpr0(
        tableRef: TableRef,
        mappers: DialectTypeMappers,
        @nowarn("msg=unused") queryable: Table.Metadata.QueryableProxy
    ): Impl[Column] =
      // TODO: we should not cache the columns here because this can be called multiple times,
      //       and each time the captured tableRef should be treated as a fresh value.
      val columns = SimpleTableMacros.computeColumns[Columns](mappers, tableRef)
      SimpleTable.Record.fromIArray(columns).asInstanceOf[Impl[Column]]

    val metadata0 = Table.Metadata[Impl](queryables, walkLabels0, queryable, vExpr0)

    SimpleTable.Metadata(metadata0)
}
