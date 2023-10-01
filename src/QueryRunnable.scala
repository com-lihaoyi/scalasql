package usql

import upickle.default.{Reader, Writer}

trait QueryRunnable[T, V]{
  def valueReader: Reader[V]
  def queryWriter: Writer[T]
  def primitive: Boolean
}

object QueryRunnable{
  implicit def containerQr[E[_] <: Expr[_], V[_[_]]](implicit valueReader0: Reader[V[Val]],
                                                     queryWriter0: Writer[V[E]]): QueryRunnable[V[E], V[Val]] = {
    new QueryRunnable[V[E], V[Val]] {
      def valueReader = valueReader0
      def queryWriter = queryWriter0
      def primitive = false
    }
  }

  implicit def exprQr[E[_] <: Expr[_], T](implicit valueReader0: Reader[T],
                                          queryWriter0: Writer[E[T]]): QueryRunnable[E[T], T] = {
    new QueryRunnable[E[T], T] {
      def valueReader = valueReader0
      def queryWriter = queryWriter0
      def primitive = true
    }
  }

  implicit def tuple2Qr[E[_] <: Expr[_], T, V](implicit valueReader0: Reader[T],
                                               queryWriter0: Writer[E[T]],
                                               valueReader02: Reader[V],
                                               queryWriter02: Writer[E[V]]): QueryRunnable[(E[T], E[V]), (T, V)] = {
    new QueryRunnable[(E[T], E[V]), (T, V)] {
      def valueReader = upickle.default.Tuple2Reader(valueReader0, valueReader02)
      def queryWriter = upickle.default.Tuple2Writer(queryWriter0, queryWriter02)
      def primitive = true
    }
  }
}
