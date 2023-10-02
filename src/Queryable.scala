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
  implicit def exprQr[E[_] <: Expr[_], T](implicit valueReader0: Reader[T],
                                          queryWriter0: Writer[E[T]]): Queryable[E[T], T] = {
    new Queryable.Simple[E[T], T](_.toTables, queryWriter0, valueReader0)
  }

  implicit def tuple2Qr[E[_] <: Expr[_], T, V](implicit valueReader0: Reader[(T, V)],
                                               queryWriter0: Writer[(E[T], E[V])]): Queryable[(E[T], E[V]), (T, V)] = {
    new Queryable.Simple[(E[T], E[V]), (T, V)](t => t._1.toTables ++ t._2.toTables, queryWriter0, valueReader0)
  }

  implicit def tuple3Qr[E[_] <: Expr[_], T, V, U](implicit valueReader0: Reader[(T, V, U)],
                                                  queryWriter0: Writer[(E[T], E[V], E[U])]): Queryable[(E[T], E[V], E[U]), (T, V, U)] = {
    new Queryable.Simple[(E[T], E[V], E[U]), (T, V, U)](
      t => t._1.toTables ++ t._2.toTables ++ t._3.toTables,
      queryWriter0,
      valueReader0
    )
  }
}
