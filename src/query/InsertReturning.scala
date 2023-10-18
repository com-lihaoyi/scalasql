package usql.query

import renderer.InsertToSql
import usql.{OptionPickler, Queryable}
import usql.renderer.{Context, SqlStr}


case class InsertReturning[Q, R](insert: Insert[_], returning: Q)(implicit val qr: Queryable[Q, R])

object InsertReturning{
  implicit def UpdateReturningQueryable[Q, R](implicit qr: Queryable[Q, R]): Queryable[InsertReturning[Q, R], Seq[R]] =
    new InsertReturningQueryable[Q, R]()(qr)

  class InsertReturningQueryable[Q, R](implicit qr: Queryable[Q, R]) extends Queryable[InsertReturning[Q, R], Seq[R]] {
    def walk(ur: InsertReturning[Q, R]): Seq[(List[String], Expr[_])] = qr.walk(ur.returning)

    override def singleRow = false

    def valueReader: OptionPickler.Reader[Seq[R]] = OptionPickler.SeqLikeReader(qr.valueReader, Vector.iterableFactory)

    override def toSqlQuery(q: InsertReturning[Q, R], ctx0: Context): SqlStr = {
      InsertToSql.returning(q, qr, ctx0.tableNameMapper, ctx0.columnNameMapper)
    }
  }

}