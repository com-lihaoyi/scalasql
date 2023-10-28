package scalasql.dialects

import scalasql.Queryable
import scalasql.query.{InsertReturnable, InsertReturning, Returnable, Returning}

trait ReturningDialect extends Dialect {
  implicit class InsertReturningConv[Q](r: InsertReturnable[Q]) {
    def returning[Q2, R](f: Q => Q2)(implicit qr: Queryable[Q2, R]): InsertReturning[Q2, R] = {
      InsertReturning.Impl(r, f(r.expr))
    }
  }

  implicit class ReturningConv[Q](r: Returnable[Q]) {
    def returning[Q2, R](f: Q => Q2)(implicit qr: Queryable[Q2, R]): Returning[Q2, R] = {
      Returning.Impl(r, f(r.expr))
    }
  }
}
