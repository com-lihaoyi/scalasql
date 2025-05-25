package scalasql.dialects

import scalasql.core.{Expr, WithSqlExpr}
import scalasql.query._

trait OnConflictOps {
  implicit def OnConflictableInsertColumns[VCol, R](
      query: InsertColumns[VCol, R]
  ): OnConflict[VCol, Int] =
    new OnConflict[VCol, Int](query, WithSqlExpr.get(query), query.table)

  implicit def OnConflictableInsertValues[VCol, R](
      query: InsertValues[VCol, R]
  ): OnConflict[VCol, Int] =
    new OnConflict[VCol, Int](query, WithSqlExpr.get(query), query.table)

  implicit def OnConflictableInsertSelect[VCol, C, R, R2](
      query: InsertSelect[VCol, C, R, R2]
  ): OnConflict[VCol, Int] = {
    new OnConflict[VCol, Int](query, WithSqlExpr.get(query), query.table)
  }

}
