package usql

import OptionPickler.{Reader, Writer}

/**
 * Typeclass to indicate that we are able to evaluate a query of type [[Q]] to
 * return a result of type [[V]]
 */
trait Queryable[Q, V]{
  def queryWriter: ExprFlattener[Q]
  def valueReader: Reader[V]
}

object Queryable{
  class Simple[T, V](val queryWriter: ExprFlattener[T],
                     val valueReader: Reader[V]) extends Queryable[T, V]{
  }

  implicit def exprQr[T](implicit valueReader0: Reader[T],
                         queryWriter0: ExprFlattener[Expr[T]]): Queryable[Expr[T], T] = {
    new Queryable.Simple[Expr[T], T](queryWriter0, valueReader0)
  }

  implicit def tuple2Qr[Q1, Q2, V1, V2](implicit q1: Queryable[Q1, V1],
                                        q2: Queryable[Q2, V2]): Queryable[(Q1, Q2), (V1, V2)] = {
    new Queryable.Simple[(Q1, Q2), (V1, V2)](
      ExprFlattener.Tuple2Flattener(q1.queryWriter, q2.queryWriter),
      OptionPickler.Tuple2Reader(q1.valueReader, q2.valueReader),
    )
  }

  implicit def tuple3Qr[Q1, Q2, Q3, V1, V2, V3](implicit q1: Queryable[Q1, V1],
                                                q2: Queryable[Q2, V2],
                                                q3: Queryable[Q3, V3]): Queryable[(Q1, Q2, Q3), (V1, V2, V3)] = {
    new Queryable.Simple[(Q1, Q2, Q3), (V1, V2, V3)](
      ExprFlattener.Tuple3Flattener(q1.queryWriter, q2.queryWriter, q3.queryWriter),
      OptionPickler.Tuple3Reader(q1.valueReader, q2.valueReader, q3.valueReader),
    )
  }
}
