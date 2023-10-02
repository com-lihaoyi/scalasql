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

  implicit def tuple2Qr[T, V](implicit valueReader0: Reader[(T, V)],
                                               queryWriter0: Writer[(Expr[T], Expr[V])]): Queryable[(Expr[T], Expr[V]), (T, V)] = {
    new Queryable.Simple[(Expr[T], Expr[V]), (T, V)](t => t._1.toTables ++ t._2.toTables, queryWriter0, valueReader0)
  }

  implicit def tuple3Qr[T, V, U](implicit valueReader0: Reader[(T, V, U)],
                                                  queryWriter0: Writer[(Expr[T], Expr[V], Expr[U])]): Queryable[(Expr[T], Expr[V], Expr[U]), (T, V, U)] = {
    new Queryable.Simple[(Expr[T], Expr[V], Expr[U]), (T, V, U)](
      t => t._1.toTables ++ t._2.toTables ++ t._3.toTables,
      queryWriter0,
      valueReader0
    )
  }

  implicit def tuple2Qr2[
    T1[_[_]] <: Product,
    T2[_[_]] <: Product
  ](
    implicit q1: Queryable[T1[Expr], T1[Val]],
    q2: Queryable[T2[Expr], T2[Val]]
  ): Queryable[(T1[Expr], T2[Expr]), (T1[Val], T2[Val])] = {
    new Queryable[(T1[Expr], T2[Expr]), (T1[Val], T2[Val])]{
      def toTables(t: (T1[Expr], T2[Expr])): Set[Table.Base] = q1.toTables(t._1) ++ q2.toTables(t._2)

      def valueReader: OptionPickler.Reader[(T1[Val], T2[Val])] = {
        OptionPickler.Tuple2Reader(q1.valueReader, q2.valueReader)
      }

      def queryWriter: OptionPickler.Writer[(T1[Expr], T2[Expr])] = {

        OptionPickler.Tuple2Writer(q1.queryWriter, q2.queryWriter)
      }
    }

  }
}
