package scalasql.query

import scalasql.{Queryable, Table}

/**
 * Wrapper class with extension methods to add support for `JOIN LATERAL`, which
 * allow for `JOIN` clauses to access the results of earlier `JOIN` and `FROM` clauses.
 * Only supported by Postgres and MySql
 */
class LateralJoinOps[C[_, _], Q, R](wrapped: JoinOps[C, Q, R] with Joinable[Q, R]) {
  object lateral {
    /**
     * Performs a `CROSS JOIN LATERAL`, similar to `CROSS JOIN` but allows the
     * `JOIN` clause to access the results of earlier `JOIN` and `FROM` clauses.
     * Only supported by Postgres and MySql
     */
    def crossJoin[Q2, R2](other: Q => Joinable[Q2, R2])(
      implicit qr: Queryable.Row[Q2, R2]
    ): C[(Q, Q2), (R, R2)] = JoinOps.join0(wrapped, "CROSS JOIN LATERAL", other(WithExpr.get(wrapped)), None)

    /**
     * Performs a `JOIN LATERAL`, similar to `JOIN` but allows the
     * `JOIN` clause to access the results of earlier `JOIN` and `FROM` clauses.
     * Only supported by Postgres and MySql
     */
    def join[Q2, R2](other: Q => Joinable[Q2, R2])(on: (Q, Q2) => Expr[Boolean])(
      implicit qr: Queryable.Row[Q2, R2]
    ): C[(Q, Q2), (R, R2)] = JoinOps.join0(wrapped, "JOIN LATERAL", other(WithExpr.get(wrapped)), Some(on))

    def crossJoin[Q2, R2](): FlatJoin.Mapper[Q, Q2, R, R2] = {
      val (from, expr) = Joinable.joinableToFromExpr(wrapped)
      new FlatJoin.Mapper[Q, Q2, R, R2]("CROSS JOIN LATERAL", from, expr, None, Nil)
    }

    def join[Q2, R2](on: Q => Expr[Boolean]): FlatJoin.Mapper[Q, Q2, R, R2] = {
      val (from, expr) = Joinable.joinableToFromExpr(wrapped)
      new FlatJoin.Mapper[Q, Q2, R, R2]("JOIN LATERAL", from, expr, Some(on(expr)), Nil)
    }

    def leftJoin[Q2, R2](on: Q => Expr[Boolean]): FlatJoin.NullableMapper[Q, Q2, R, R2] = {
      val (from, expr) = Joinable.joinableToFromExpr(wrapped)
      new FlatJoin.NullableMapper[Q, Q2, R, R2]("LEFT JOIN LATERAL", from, JoinNullable(expr), Some(on(expr)), Nil)
    }
    //    /**
//     * Performs a `LEFT JOIN` on the given [[other]], typically a [[Table]] or [[Select]].
//     */
//    def leftJoin[Q2, R2](other: Joinable[Q2, R2])(on: (Q, Q2) => Expr[Boolean])(
//      implicit joinQr: Queryable.Row[Q2, R2]
//    ): Select[(Q, JoinNullable[Q2]), (R, Option[R2])]
//
//    /**
//     * Performs a `RIGHT JOIN` on the given [[other]], typically a [[Table]] or [[Select]].
//     */
//    def rightJoin[Q2, R2](other: Joinable[Q2, R2])(on: (Q, Q2) => Expr[Boolean])(
//      implicit joinQr: Queryable.Row[Q2, R2]
//    ): Select[(JoinNullable[Q], Q2), (Option[R], R2)]
//
//    /**
//     * Performs a `OUTER JOIN` on the given [[other]], typically a [[Table]] or [[Select]].
//     */
//    def outerJoin[Q2, R2](other: Joinable[Q2, R2])(on: (Q, Q2) => Expr[Boolean])(
//      implicit joinQr: Queryable.Row[Q2, R2]
//    ): Select[(JoinNullable[Q], JoinNullable[Q2]), (Option[R], Option[R2])]
  }
}
