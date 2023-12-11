package scalasql.dialects

import scalasql.core.{WithSqlExpr, Queryable}
import scalasql.query.{OnConflict, Returning}

trait ReturningDialect extends Dialect {
  implicit class InsertReturningConv[Q](r: Returning.InsertBase[Q]) {
    def returning[Q2, R](f: Q => Q2)(implicit qr: Queryable.Row[Q2, R]): Returning[Q2, R] = {
      new Returning.InsertImpl(r, f(WithSqlExpr.get(r)))
    }
  }

  implicit class ReturningConv[Q](r: Returning.Base[Q]) {
    def returning[Q2, R](f: Q => Q2)(implicit qr: Queryable.Row[Q2, R]): Returning[Q2, R] = {
      new Returning.Impl(r, f(WithSqlExpr.get(r)))
    }
  }
  implicit class OnConflictUpdateConv[Q, R](r: OnConflict.Update[Q, R]) {
    def returning[Q2, R](f: Q => Q2)(implicit qr: Queryable.Row[Q2, R]): Returning[Q2, R] = {
      new Returning.Impl(r, f(WithSqlExpr.get(r)))
    }
  }
  implicit class OnConflictIgnoreConv[Q, R](r: OnConflict.Ignore[Q, R]) {
    def returning[Q2, R](f: Q => Q2)(implicit qr: Queryable.Row[Q2, R]): Returning[Q2, R] = {
      new Returning.Impl(r, f(WithSqlExpr.get(r)))
    }
  }
}
