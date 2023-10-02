package usql

import upickle.default.{Reader, Writer}

trait Queryable[T, V]{
  def toTables(t: T): Set[Table.Base]
  def valueReader: Reader[V]
  def queryWriter: Writer[T]
}

object Queryable{
  implicit def exprQr[E[_] <: Expr[_], T](implicit valueReader0: Reader[T],
                                          queryWriter0: Writer[E[T]]): Queryable[E[T], T] = {
    new Queryable[E[T], T] {
      def toTables(t: E[T]): Set[Table.Base] = t.toTables
      def valueReader = valueReader0
      def queryWriter = queryWriter0
    }
  }

  implicit def tuple2Qr[E[_] <: Expr[_], T, V](implicit valueReader0: Reader[T],
                                               queryWriter0: Writer[E[T]],
                                               valueReader02: Reader[V],
                                               queryWriter02: Writer[E[V]]): Queryable[(E[T], E[V]), (T, V)] = {
    new Queryable[(E[T], E[V]), (T, V)] {
      def toTables(t: (E[T], E[V])): Set[Table.Base] = t._1.toTables ++ t._2.toTables
      def valueReader = upickle.default.Tuple2Reader(valueReader0, valueReader02)
      def queryWriter = upickle.default.Tuple2Writer(queryWriter0, queryWriter02)
    }
  }
  implicit def tuple3Qr[E[_] <: Expr[_], T, V, U](implicit valueReader0: Reader[T],
                                                  queryWriter0: Writer[E[T]],
                                                  valueReader02: Reader[V],
                                                  queryWriter02: Writer[E[V]],
                                                  valueReader03: Reader[U],
                                                  queryWriter03: Writer[E[U]]): Queryable[(E[T], E[V], E[U]), (T, V, U)] = {
    new Queryable[(E[T], E[V], E[U]), (T, V, U)] {
      def toTables(t: (E[T], E[V], E[U])): Set[Table.Base] = t._1.toTables ++ t._2.toTables ++ t._3.toTables
      def valueReader = upickle.default.Tuple3Reader(valueReader0, valueReader02, valueReader03)
      def queryWriter = upickle.default.Tuple3Writer(queryWriter0, queryWriter02, queryWriter03)
    }
  }
}
