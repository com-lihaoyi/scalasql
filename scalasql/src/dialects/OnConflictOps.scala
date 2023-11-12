package scalasql.dialects

import scalasql.query._

trait OnConflictOps {
  implicit def OnConflictableInsertValues[Q, R](query: InsertValues[Q, R]): OnConflict[Q, Int] =
    new OnConflict[Q, Int](query, WithExpr.get(query), query.table)

  implicit def OnConflictableInsertSelect[Q, C, R, R2](
      query: InsertSelect[Q, C, R, R2]
  ): OnConflict[Q, Int] =
    new OnConflict[Q, Int](query, WithExpr.get(query), query.table)

}
