package usql

import OptionPickler.{Reader, Writer}

trait Queryable[T, V]{
  def toTables(t: T): Set[Table.Base]
  def valueReader: Reader[V]
  def queryWriter: Writer[T]
}

object Queryable{
  class Simple[T, V](toTables0: T => Set[Table.Base],
                     val queryWriter: Writer[T],
                     val valueReader: Reader[V]) extends Queryable[T, V]{
    def toTables(t: T) = toTables0(t)
  }

  implicit def exprQr[T](implicit valueReader0: Reader[T],
                         queryWriter0: Writer[Expr[T]]): Queryable[Expr[T], T] = {
    new Queryable.Simple[Expr[T], T](_.toTables, queryWriter0, valueReader0)
  }

  implicit def tuple2Qr[E1, E2, T1, T2](implicit q1: Queryable[E1, T1],
                                        q2: Queryable[E2, T2]): Queryable[(E1, E2), (T1, T2)] = {
    new Queryable.Simple[(E1, E2), (T1, T2)](
      t => q1.toTables(t._1) ++ q2.toTables(t._2),
      OptionPickler.Tuple2Writer(q1.queryWriter, q2.queryWriter),
      OptionPickler.Tuple2Reader(q1.valueReader, q2.valueReader),
    )
  }

  implicit def tuple3Qr[E1, E2, E3, T1, T2, T3](implicit q1: Queryable[E1, T1],
                                                q2: Queryable[E2, T2],
                                                q3: Queryable[E3, T3]): Queryable[(E1, E2, E3), (T1, T2, T3)] = {
    new Queryable.Simple[(E1, E2, E3), (T1, T2, T3)](
      t => q1.toTables(t._1) ++ q2.toTables(t._2) ++ q3.toTables(t._3),
      OptionPickler.Tuple3Writer(q1.queryWriter, q2.queryWriter, q3.queryWriter),
      OptionPickler.Tuple3Reader(q1.valueReader, q2.valueReader, q3.valueReader),
    )
  }
}
