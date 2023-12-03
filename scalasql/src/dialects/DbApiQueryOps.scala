package scalasql.dialects

import scalasql.core.SqlStr.SqlStringSyntax
import scalasql.core._
import scalasql.query.{Select, Values, WithCte, WithCteRef}

class DbApiQueryOps(dialect: DialectBase) {
  import dialect._

  /**
   * Creates a SQL `VALUES` clause
   */
  def values[Q, R](ts: Seq[R])(implicit qr: Queryable.Row[Q, R]): Values[Q, R] =
    new scalasql.query.Values(ts)

  import scalasql.core.SqlStr.SqlStringSyntax

  /** Generates a SQL `WITH` common table expression clause */
  def withCte[Q, Q2, R, R2](
      lhs: Select[Q, R]
  )(block: Select[Q, R] => Select[Q2, R2])(implicit qr: Queryable.Row[Q2, R2]): Select[Q2, R2] = {
    val lhsSubQueryRef = new WithCteRef()
    val rhsSelect = new WithCte.Proxy[Q, R](lhs, lhsSubQueryRef, lhs.qr, dialect)

    new WithCte(lhs, lhsSubQueryRef, block(rhsSelect))
  }
}
