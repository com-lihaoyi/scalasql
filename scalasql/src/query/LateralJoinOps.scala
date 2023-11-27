package scalasql.query

import scalasql.{Queryable, Table}

/**
 * Wrapper class with extension methods to add support for `JOIN LATERAL`, which
 * allow for `JOIN` clauses to access the results of earlier `JOIN` and `FROM` clauses.
 * Only supported by Postgres and MySql
 */
class LateralJoinOps[C[_, _], Q, R](wrapped: JoinOps[C, Q, R] with Joinable[Q, R])(
    implicit qr: Queryable.Row[Q, R]
) {

  /**
   * Performs a `CROSS JOIN LATERAL`, similar to `CROSS JOIN` but allows the
   * `JOIN` clause to access the results of earlier `JOIN` and `FROM` clauses.
   * Only supported by Postgres and MySql
   */
  def crossJoinLateral[Q2, R2, QF, RF](other: Q => Joinable[Q2, R2])(
    implicit ja: JoinAppend[Q, R, Q2, R2, QF, RF],
  ): C[QF, RF] =
    JoinOps.join0(wrapped, "CROSS JOIN LATERAL", other(WithExpr.get(wrapped)), None)

  /**
   * Performs a `JOIN LATERAL`, similar to `JOIN` but allows the
   * `JOIN` clause to access the results of earlier `JOIN` and `FROM` clauses.
   * Only supported by Postgres and MySql
   */
  def joinLateral[Q2, R2, QF, RF](other: Q => Joinable[Q2, R2])(on: (Q, Q2) => Expr[Boolean])(
    implicit ja: JoinAppend[Q, R, Q2, R2, QF, RF],
  ): C[QF, RF] =
    JoinOps.join0(wrapped, "JOIN LATERAL", other(WithExpr.get(wrapped)), Some(on))

  def leftJoinLateral[Q2, R2](other: Q => Joinable[Q2, R2])(
      on: (Q, Q2) => Expr[Boolean]
  )(implicit joinQr: Queryable.Row[Q2, R2]): Select[(Q, JoinNullable[Q2]), (R, Option[R2])] = {
    wrapped.joinCopy2(other(WithExpr.get(wrapped)), Some(on), "LEFT JOIN LATERAL")((e, o) =>
      (e, JoinNullable(o))
    )
  }

  /**
   * Version of `crossJoinLateral` meant for use in `for`-comprehensions
   */
  def crossJoinLateral[Q2, R2](): FlatJoin.Mapper[Q, Q2, R, R2] = {
    val (from, expr) = Joinable.joinableToFromExpr(wrapped)
    new FlatJoin.Mapper[Q, Q2, R, R2]("CROSS JOIN LATERAL", from, expr, None, Nil)
  }

  /**
   * Version of `joinLateral` meant for use in `for`-comprehensions
   */
  def joinLateral[Q2, R2](on: Q => Expr[Boolean]): FlatJoin.Mapper[Q, Q2, R, R2] = {
    val (from, expr) = Joinable.joinableToFromExpr(wrapped)
    new FlatJoin.Mapper[Q, Q2, R, R2]("JOIN LATERAL", from, expr, Some(on(expr)), Nil)
  }

  /**
   * Version of `leftJoinLateral` meant for use in `for`-comprehensions
   */
  def leftJoinLateral[Q2, R2](on: Q => Expr[Boolean]): FlatJoin.NullableMapper[Q, Q2, R, R2] = {
    val (from, expr) = Joinable.joinableToFromExpr(wrapped)
    new FlatJoin.NullableMapper[Q, Q2, R, R2](
      "LEFT JOIN LATERAL",
      from,
      JoinNullable(expr),
      Some(on(expr)),
      Nil
    )
  }
}
