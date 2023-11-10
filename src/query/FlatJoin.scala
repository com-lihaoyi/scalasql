package scalasql.query

import scalasql.Queryable


object FlatJoin{
  trait Rhs[Q2, R2]
  class MapResult[Q, Q2, R, R2](
      val from: From,
      val on: Expr[Boolean],
      val qr: Queryable.Row[Q2, R2],
      val f: Q2,
      val where: Seq[Expr[Boolean]]
  ) extends Rhs[Q2, R2]

  class FlatMapResult[Q, Q2, R, R2](
      val from: From,
      val on: Expr[Boolean],
      val qr: Queryable.Row[Q2, R2],
      val f: Rhs[Q2, R2],
      val where: Seq[Expr[Boolean]]
  ) extends Rhs[Q2, R2]

  class Mapper[Q, Q2, R, R2](
      from: From,
      expr: Q,
      on: Expr[Boolean],
      where: Seq[Expr[Boolean]]
  ) {
    def map(f: Q => Q2)(implicit qr: Queryable.Row[Q2, R2]): MapResult[Q, Q2, R, R2] = {
      new MapResult[Q, Q2, R, R2](from, on, qr, f(expr), where)
    }

    def flatMap(
        f: Q => Rhs[Q2, R2]
    )(implicit qr: Queryable.Row[Q2, R2]): FlatMapResult[Q, Q2, R, R2] = {
      new FlatMapResult[Q, Q2, R, R2](from, on, qr, f(expr), where)
    }

    def withFilter(x: Q => Expr[Boolean]): Mapper[Q, Q2, R, R2] =
      new Mapper(from, expr, on, where ++ Seq(x(expr)))
  }
}
