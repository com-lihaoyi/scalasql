package scalasql.query

import scalasql.core.{Context, JoinNullable, Queryable, Expr}

object FlatJoin {
  trait Rhs[Q2, R2]
  class MapResult[Q, Q2, R, R2](
      val prefix: String,
      val from: Context.From,
      val on: Option[Expr[Boolean]],
      val qr: Queryable.Row[Q2, R2],
      val f: Q2,
      val where: Seq[Expr[Boolean]]
  ) extends Rhs[Q2, R2]

  class FlatMapResult[Q, Q2, R, R2](
      val prefix: String,
      val from: Context.From,
      val on: Option[Expr[Boolean]],
      val qr: Queryable.Row[Q2, R2],
      val f: Rhs[Q2, R2],
      val where: Seq[Expr[Boolean]]
  ) extends Rhs[Q2, R2]

  class Mapper[Q, Q2, R, R2](
      prefix: String,
      from: Context.From,
      expr: Q,
      on: Option[Expr[Boolean]],
      where: Seq[Expr[Boolean]]
  ) {
    def map(f: Q => Q2)(implicit qr: Queryable.Row[Q2, R2]): MapResult[Q, Q2, R, R2] = {
      new MapResult[Q, Q2, R, R2](prefix, from, on, qr, f(expr), where)
    }

    def flatMap(
        f: Q => Rhs[Q2, R2]
    )(implicit qr: Queryable.Row[Q2, R2]): FlatMapResult[Q, Q2, R, R2] = {
      new FlatMapResult[Q, Q2, R, R2](prefix, from, on, qr, f(expr), where)
    }

    def filter(x: Q => Expr[Boolean]): Mapper[Q, Q2, R, R2] = withFilter(x)
    def withFilter(x: Q => Expr[Boolean]): Mapper[Q, Q2, R, R2] =
      new Mapper(prefix, from, expr, on, where ++ Seq(x(expr)))
  }
  class NullableMapper[Q, Q2, R, R2](
      prefix: String,
      from: Context.From,
      expr: JoinNullable[Q],
      on: Option[Expr[Boolean]],
      where: Seq[Expr[Boolean]]
  ) {
    def lateral = new NullableMapper[Q, Q2, R, R2](prefix + " LATERAL", from, expr, on, where)
    def map(
        f: JoinNullable[Q] => Q2
    )(implicit qr: Queryable.Row[Q2, R2]): MapResult[Q, Q2, R, R2] = {
      new MapResult[Q, Q2, R, R2](prefix, from, on, qr, f(expr), where)
    }

    def flatMap(
        f: JoinNullable[Q] => Rhs[Q2, R2]
    )(implicit qr: Queryable.Row[Q2, R2]): FlatMapResult[Q, Q2, R, R2] = {
      new FlatMapResult[Q, Q2, R, R2](prefix, from, on, qr, f(expr), where)
    }

    def filter(x: JoinNullable[Q] => Expr[Boolean]): NullableMapper[Q, Q2, R, R2] = withFilter(x)
    def withFilter(x: JoinNullable[Q] => Expr[Boolean]): NullableMapper[Q, Q2, R, R2] =
      new NullableMapper("LEFT JOIN", from, expr, on, where ++ Seq(x(expr)))
  }
}
