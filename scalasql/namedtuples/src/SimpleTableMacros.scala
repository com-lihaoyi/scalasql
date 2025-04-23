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

  // inline def computeLabels[N <: Tuple, V <: Tuple](acc: List[Seq[String]]): List[String] = {
  //   inline compiletime.erasedValue[(N, V)] match
  //     case _: (n *: ns, v *: vs) => computeLabels(singleLabel[n, v] :: acc)
  //     case _: (EmptyTuple, EmptyTuple) => acc.reverse.flatten
  // }

  // private inline def singleLabel[N, V]: Seq[String] = {
  //   compiletime.summonFrom {
  //     case m: Table.ImplicitMetadata[SimpleTable.NamedTupleOf[V]] =>
  //       m.value.walkLabels0()
  //     case _ =>
  //       List(compiletime.constValue[N].asInstanceOf[String])
  //   }
  // }
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

  // def composeLabels(labels0: Tuple, optMetas0: Tuple): IndexedSeq[String] = {
  //   val labels = asIArray[String](labels0)
  //   val optMetas = asIArray[BaseMetadata[?]](optMetas0)
  //   labels.zip(optMetas).map { (name, opt) =>
  //     opt.value match {
  //       case Some(m) => m.metadata0.walkLabels0()
  //       case None    => List(name)
  //     }
  //   }.flatten
  // }
}

trait SimpleTableMacros {
  transparent inline given initTableMetadata[C]: SimpleTable.Metadata[C] =
    // val m = compiletime.summonInline[Mirror.Of[NamedTuple.From[C]]]
    type Impl = SimpleTable.NamedTupleOf[C]
    type Labels = NamedTuple.Names[NamedTuple.From[C]]
    type Values = NamedTuple.DropNames[NamedTuple.From[C]]
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
        mappers: DialectTypeMappers,
        queryable: Table.Metadata.QueryableProxy
    ): Queryable[Impl[Expr], Impl[Sc]] = ???
    def vExpr0(
        tableRef: TableRef,
        mappers: DialectTypeMappers,
        queryable: Table.Metadata.QueryableProxy
    ): Impl[Column] = ???

    val metadata0 = Table.Metadata[Impl](queryables, walkLabels0, queryable, vExpr0)

    SimpleTable.Metadata(metadata0)
}
