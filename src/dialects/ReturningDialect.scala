package scalasql.dialects

import scalasql.Queryable
import scalasql.query.{Returnable, Returning}

trait ReturningDialect extends Dialect {
  implicit class ReturningConv[Q](r: Returnable[Q]){
    def returning[Q2, R](f: Q => Q2)(implicit qr: Queryable[Q2, R]): Returning[Q2, R] = {
      Returning(r, f(r.expr))
    }
  }
}
