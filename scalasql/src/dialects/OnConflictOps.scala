package scalasql.dialects

import scalasql.Column
import scalasql.query._

trait OnConflictOps {
  implicit def OnConflictableInsertValues[V[_[_]], R](query: InsertColumns[V, R]): OnConflict[V[Column.ColumnExpr], Int] =
    new OnConflict[V[Column.ColumnExpr], Int](query, WithExpr.get(query), query.table)

  implicit def OnConflictableInsertSelect[V[_[_]], C, R, R2](
      query: InsertSelect[V, C, R, R2]
  ): OnConflict[V[Column.ColumnExpr], Int] = {
    new OnConflict[V[Column.ColumnExpr], Int](
      query.asInstanceOf[Query[Int] with InsertReturnable[V[Column.ColumnExpr]]],
      WithExpr.get(query).asInstanceOf[V[Column.ColumnExpr]],
      query.table
    )
  }

}
