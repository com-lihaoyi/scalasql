package usql.query

import usql.renderer.SqlStr.SqlStringSyntax
import usql.renderer.{Context, ExprsToSql, SelectToSql, SqlStr, UpdateToSql}
import usql.{OptionPickler, Queryable}

trait Returnable[Q] {
  def expr: Q
  def table: TableRef
  def toSqlQuery(implicit ctx: Context): SqlStr
  def returning[Q2, R](f: Q => Q2)(implicit qr: Queryable[Q2, R]): Returning[Q2, R] = {
    Returning(this, f(expr))
  }
}

case class Returning[Q, R](returnable: Returnable[_], returning: Q)(implicit
    val qr: Queryable[Q, R]
)

object Returning {
  implicit def UpdateReturningQueryable[Q, R](implicit
      qr: Queryable[Q, R]
  ): Queryable[Returning[Q, R], Seq[R]] =
    new InsertReturningQueryable[Q, R]()(qr)

  class InsertReturningQueryable[Q, R](implicit qr: Queryable[Q, R])
      extends Queryable[Returning[Q, R], Seq[R]] {
    def walk(ur: Returning[Q, R]): Seq[(List[String], Expr[_])] = qr.walk(ur.returning)

    override def singleRow = false

    def valueReader: OptionPickler.Reader[Seq[R]] =
      OptionPickler.SeqLikeReader(qr.valueReader, Vector.iterableFactory)

    override def toSqlQuery(q: Returning[Q, R], ctx0: Context): SqlStr = {
      implicit val (_, _, _, ctx) = SelectToSql.computeContext(
        ctx0.tableNameMapper,
        ctx0.columnNameMapper,
        Nil,
        Some(q.returnable.table),
        Map(),
        ctx0.mySqlUpdateJoinSyntax
      )

      val prefix = q.returnable.toSqlQuery
      val (flattenedExpr, exprStr) = ExprsToSql.apply0(qr.walk(q.returning), ctx, usql"")
      val suffix = usql" RETURNING $exprStr"

      prefix + suffix
    }
  }

}
