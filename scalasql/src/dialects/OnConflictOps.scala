package scalasql.dialects

import scalasql.Column
import scalasql.query._

trait OnConflictOps {
  implicit def OnConflictableInsertValues[V[_[_]], R](
      query: InsertColumns[V, R]
  ): OnConflict[V[Column], Int] =
    new OnConflict[V[Column], Int](query, WithExpr.get(query), query.table)

  implicit def OnConflictableInsertSelect[V[_[_]], C, R, R2](
      query: InsertSelect[V, C, R, R2]
  ): OnConflict[V[Sql], Int] = {
    new OnConflict[V[Sql], Int](query, WithExpr.get(query), query.table)
  }

}
