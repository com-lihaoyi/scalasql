package scalasql.query

import scalasql.Queryable

/**
 * Wrapper class with extension methods to add support for `JOIN LATERAL`, which
 * allow for `JOIN` clauses to access the results of earlier `JOIN` and `FROM` clauses.
 * Only supported by Postgres and MySql
 */
class LateralJoinOps[C[_, _], Q, R](wrapped: JoinOps[C, Q, R]) {
  /**
   * Performs a `CROSS JOIN LATERAL`, similar to `CROSS JOIN` but allows the
   * `JOIN` clause to access the results of earlier `JOIN` and `FROM` clauses.
   * Only supported by Postgres and MySql
   */
  def crossJoinLateral[Q2, R2](other: Q => Joinable[Q2, R2])(
    implicit qr: Queryable.Row[Q2, R2]
  ): C[(Q, Q2), (R, R2)] = JoinOps.join0(wrapped, "CROSS JOIN LATERAL", other(WithExpr.get(wrapped)), None)

  /**
   * Performs a `JOIN LATERAL`, similar to `JOIN` but allows the
   * `JOIN` clause to access the results of earlier `JOIN` and `FROM` clauses.
   * Only supported by Postgres and MySql
   */
  def joinLateral[Q2, R2](other: Q => Joinable[Q2, R2])(on: (Q, Q2) => Expr[Boolean])(
    implicit qr: Queryable.Row[Q2, R2]
  ): C[(Q, Q2), (R, R2)] = JoinOps.join0(wrapped, "JOIN LATERAL", other(WithExpr.get(wrapped)), Some(on))

}
