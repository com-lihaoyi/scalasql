package scalasql.namedtuples

import scala.NamedTuple.NamedTuple
import scalasql.core.{Queryable, Expr}

object NamedTupleQueryable {

  /** A sequence of n `Queryable.Row[Q, R]` instances, where `X` corresponds to all the `Q` and `Y` to all the `R` */
  opaque type Rows[X <: Tuple, +Y <: Tuple] = List[Queryable.Row[?, ?]]

  object Rows {
    // currently "traditional" recursive implicit search
    // because we only know the Q type, and it appears compiletime.summonAll cannot be used to refined the R type
    // e.g. compiletime.summonAll[Tuple.Map[Qs, [X] =>> Queryable.Row[X, ?]]] does nothing because its static type is fixed.
    given concatRows: [Q, R, Qs <: Tuple, Rs <: Tuple]
      => (x: Queryable.Row[Q, R])
      => (xs: Rows[Qs, Rs])
      => Rows[Q *: Qs, R *: Rs] =
      x :: xs

    given emptyRows: Rows[EmptyTuple, EmptyTuple] = Nil
  }

  /**
   * A `Queryable.Row` instance for an arbitrary named tuple type, can be derived even
   * when one of `X` or `Y` is unknown.
   */
  given NamedTupleRow: [N <: Tuple, X <: Tuple, Y <: Tuple]
    => (rs: Rows[X, Y])
    => Queryable.Row[NamedTuple[N, X], NamedTuple[N, Y]] =
    NamedTupleRowImpl[N, X, Y](rs)

  private final class NamedTupleRowImpl[
      N <: Tuple,
      X <: Tuple,
      Y <: Tuple
  ](
      rs: List[Queryable.Row[?, ?]]
  ) extends Queryable.Row[NamedTuple[N, X], NamedTuple[N, Y]]:
    def walkExprs(q: NamedTuple[N, X]): Seq[Expr[?]] = {
      val walkExprs0 = {
        val ps = q.toTuple.productIterator
        rs.iterator
          .zip(ps)
          .map({ (row, p) =>
            type Q
            type R
            val q = p.asInstanceOf[Q]
            row.asInstanceOf[Queryable.Row[Q, R]].walkExprs(q)
          })
      }

      walkExprs0.zipWithIndex
        .map { case (v, i) => (i.toString, v) }
        .flatMap { case (prefix, vs0) => vs0 }
        .toIndexedSeq
    }
    def walkLabels(): Seq[List[String]] = {
      val walkLabels0 = rs.iterator.map(_.walkLabels())
      walkLabels0.zipWithIndex
        .map { case (v, i) => (i.toString, v) }
        .flatMap { case (prefix, vs0) => vs0.map { k => prefix +: k } }
        .toIndexedSeq
    }
    def construct(args: scalasql.core.Queryable.ResultSetIterator): NamedTuple.NamedTuple[N, Y] =
      val data = IArray.from(rs.iterator.map(_.construct(args)))
      Tuple.fromIArray(data).asInstanceOf[NamedTuple.NamedTuple[N, Y]]

    def deconstruct(r: NamedTuple.NamedTuple[N, Y]): NamedTuple.NamedTuple[N, X] =
      val data = IArray.from {
        val ps = r.toTuple.productIterator
        rs.iterator
          .zip(ps)
          .map({ (row, p) =>
            type Q
            type R
            val r = p.asInstanceOf[R]
            row.asInstanceOf[Queryable.Row[Q, R]].deconstruct(r)
          })
      }
      Tuple.fromIArray(data).asInstanceOf[NamedTuple.NamedTuple[N, X]]

}
