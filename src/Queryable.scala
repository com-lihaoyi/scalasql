package usql

import OptionPickler.Reader

/**
 * Typeclass to indicate that we are able to evaluate a query of type [[Q]] to
 * return a result of type [[R]]. Involves two operations: flattening a structured
 * query to a flat list of expressions via [[walk]], and reading a JSON-ish
 * tree-shaped blob back into a return value via [[valueReader]]
 */
trait Queryable[Q, R]{
  def walk(q: Q): Seq[(List[String], Expr[_])]
  def valueReader: Reader[R]
}

object Queryable{
  def walkIndexed(items: Seq[Seq[(List[String], Expr[_])]]) = {
    walkPrefixed(items.zipWithIndex.map{case (v, i) => (i.toString, v)})
  }

  def walkPrefixed(items: Seq[(String, Seq[(List[String], Expr[_])])]) = {
    items.flatMap { case (prefix, vs0) => vs0.map { case (k, v) => (prefix +: k, v) } }
  }

  class TupleNQueryable[Q, V](val walk0: Q => Seq[Seq[(List[String], Expr[_])]],
                              val valueReader: Reader[V]) extends Queryable[Q, V]{
    def walk(q: Q) = walkIndexed(walk0(q))
  }

  implicit def exprQr[T](implicit valueReader0: Reader[T]): Queryable[Expr[T], T] = {
    new Queryable[Expr[T], T]{
      def walk(q: Expr[T]) = Seq(Nil -> q)
      def valueReader = valueReader0
    }
  }

  implicit def Tuple2Queryable[
    Q1, Q2, V1, V2
  ](implicit q1: Queryable[Q1, V1],
    q2: Queryable[Q2, V2]): Queryable[(Q1, Q2), (V1, V2)] = {
    new Queryable.TupleNQueryable(
      t => Seq(q1.walk(t._1), q2.walk(t._2)),
      OptionPickler.Tuple2Reader(q1.valueReader, q2.valueReader),
    )
  }

  implicit def Tuple3Queryable[
    Q1, Q2, Q3, V1, V2, V3
  ](implicit q1: Queryable[Q1, V1],
    q2: Queryable[Q2, V2],
    q3: Queryable[Q3, V3]): Queryable[(Q1, Q2, Q3), (V1, V2, V3)] = {
    new Queryable.TupleNQueryable(
      t => Seq(q1.walk(t._1), q2.walk(t._2), q3.walk(t._3)),
      OptionPickler.Tuple3Reader(q1.valueReader, q2.valueReader, q3.valueReader),
    )
  }

  implicit def Tuple4Queryable[
    Q1, Q2, Q3, Q4, V1, V2, V3, V4
  ](implicit q1: Queryable[Q1, V1],
    q2: Queryable[Q2, V2],
    q3: Queryable[Q3, V3],
    q4: Queryable[Q4, V4]): Queryable[(Q1, Q2, Q3, Q4), (V1, V2, V3, V4)] = {
    new Queryable.TupleNQueryable(
      t => Seq(q1.walk(t._1), q2.walk(t._2), q3.walk(t._3), q4.walk(t._4)),
      OptionPickler.Tuple4Reader(q1.valueReader, q2.valueReader, q3.valueReader, q4.valueReader),
    )
  }

  implicit def Tuple5Queryable[
    Q1, Q2, Q3, Q4, Q5, V1, V2, V3, V4, V5
  ](implicit q1: Queryable[Q1, V1],
    q2: Queryable[Q2, V2],
    q3: Queryable[Q3, V3],
    q4: Queryable[Q4, V4],
    q5: Queryable[Q5, V5]): Queryable[(Q1, Q2, Q3, Q4, Q5), (V1, V2, V3, V4, V5)] = {
    new Queryable.TupleNQueryable(
      t => Seq(q1.walk(t._1), q2.walk(t._2), q3.walk(t._3), q4.walk(t._4), q5.walk(t._5)),
      OptionPickler.Tuple5Reader(q1.valueReader, q2.valueReader, q3.valueReader, q4.valueReader, q5.valueReader),
    )
  }
}
