package usql.query

import usql.renderer.SqlStr.SqlStringSyntax
import usql.renderer.{Context, ExprsToSql, SelectToSql, SqlStr, UpdateToSql}
import usql.Queryable
import usql.utils.OptionPickler

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
) extends Query {
  def walk() = qr.walk(returning)

  override def singleRow = false

  override def toSqlQuery(implicit ctx0: Context): SqlStr = toSqlQuery0(ctx0)
  def toSqlQuery0(ctx0: Context): SqlStr = {
    implicit val (_, _, _, ctx) = SelectToSql.computeContext(
      ctx0.tableNameMapper,
      ctx0.columnNameMapper,
      Nil,
      Some(returnable.table),
      Map()
    )

    val prefix = returnable.toSqlQuery
    val (flattenedExpr, exprStr) = ExprsToSql.apply0(qr.walk(returning), ctx, usql"")
    val suffix = usql" RETURNING $exprStr"

    prefix + suffix
  }
}

object Returning {
  implicit def UpdateReturningQueryable[Q, R](implicit
      qr: Queryable[Q, R]
  ): Queryable[Returning[Q, R], Seq[R]] =
    new Query.Queryable[Returning[Q, R], Seq[R]]()(OptionPickler.SeqLikeReader(
      qr.valueReader,
      implicitly
    ))

}
