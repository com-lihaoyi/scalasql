package scalasql.namedtuples

import scalasql.query.Table
import scalasql.core.DialectTypeMappers
import scalasql.core.Queryable
import scalasql.query.Column
import scalasql.core.Sc
import scalasql.core.Expr

class SimpleTable[C]()(
    using name: sourcecode.Name,
    metadata0: SimpleTable.Metadata[C]
) extends Table[SimpleTable.NamedTupleOf[C]](using name, metadata0.metadata0)
    with SimpleTable.LowPri[C] {
  type Impl = SimpleTable.NamedTupleOf[C]
  given simpleTableImplicitMetadata: SimpleTable.WrappedMetadata[C] =
    SimpleTable.WrappedMetadata(metadata0)
  // given containerQr: (dialect: DialectTypeMappers) => Queryable.Row[Impl[Expr], Impl[Sc]] =
  //   super.containerQr.asInstanceOf[Queryable.Row[Impl[Expr], Impl[Sc]]]
  // tableMetadata
  //   .queryable(
  //     tableMetadata.walkLabels0,
  //     dialect,
  //     new Table.Metadata.QueryableProxy(tableMetadata.queryables(dialect, _))
  //   )
  //   .asInstanceOf[Queryable.Row[V[Expr], V[Sc]]]
}

object SimpleTable {
  trait LowPri[C] { this: SimpleTable[C] =>
    // given containerQr2: (dialect: DialectTypeMappers) => Queryable.Row[Impl[Column], Impl[Sc]] =
    //   containerQr.asInstanceOf[Queryable.Row[Impl[Column], Impl[Sc]]]
  }

  opaque type WrappedMetadata[C] = Metadata[C]
  object WrappedMetadata {
    def apply[C](metadata: Metadata[C]): WrappedMetadata[C] = metadata
    extension [C](m: WrappedMetadata[C]) {
      def metadata: Metadata[C] = m
    }
  }
  class Metadata[C](val metadata0: Table.Metadata[NamedTupleOf[C]]):
    def rowExpr(
        mappers: DialectTypeMappers
    ): Queryable.Row[NamedTupleOf[C][Expr], NamedTupleOf[C][Sc]] =
      metadata0
        .queryable(
          metadata0.walkLabels0,
          mappers,
          new Table.Metadata.QueryableProxy(metadata0.queryables(mappers, _))
        )
        .asInstanceOf[Queryable.Row[NamedTupleOf[C][Expr], NamedTupleOf[C][Sc]]]
  object Metadata extends SimpleTableMacros

  type NamedTupleOf[C] = [T[_]] =>> NamedTuple.Map[NamedTuple.From[C], T]
}
