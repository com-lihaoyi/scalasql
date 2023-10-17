package usql.query

import usql.renderer.{Context, SqlStr, UpdateToSql}
import usql.{OptionPickler, Queryable}


case class UpdateReturning[Q, R](update: Update[_], returning: Q)(implicit val qr: Queryable[Q, R])

object UpdateReturning{
  implicit def UpdateReturningQueryable[Q, R](implicit qr: Queryable[Q, R]): Queryable[UpdateReturning[Q, R], Seq[R]] =
    new UpdateReturningQueryable[Q, R]()(qr)

  class UpdateReturningQueryable[Q, R](implicit qr: Queryable[Q, R]) extends Queryable[UpdateReturning[Q, R], Seq[R]] {
    def walk(ur: UpdateReturning[Q, R]): Seq[(List[String], Expr[_])] = qr.walk(ur.returning)

    override def singleRow = false

    def valueReader: OptionPickler.Reader[Seq[R]] = OptionPickler.SeqLikeReader(qr.valueReader, Vector.iterableFactory)

    override def toSqlQuery(q: UpdateReturning[Q, R], ctx0: Context): SqlStr = {
      UpdateToSql.returning(q, qr, ctx0.tableNameMapper, ctx0.columnNameMapper)
    }
  }

}