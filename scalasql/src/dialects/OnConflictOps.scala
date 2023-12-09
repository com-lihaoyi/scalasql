package scalasql.dialects

import scalasql.core.{Db, WithSqlExpr}
import scalasql.query._

trait OnConflictOps {
  implicit def OnConflictableInsertValues[V[_[_]], R](
      query: InsertColumns[V, R]
  ): OnConflict[V[Column], Int] =
    new OnConflict[V[Column], Int](query, WithSqlExpr.get(query), query.table)

  implicit def OnConflictableInsertSelect[V[_[_]], C, R, R2](
      query: InsertSelect[V, C, R, R2]
  ): OnConflict[V[Db], Int] = {
    new OnConflict[V[Db], Int](query, WithSqlExpr.get(query), query.table)
  }

}
