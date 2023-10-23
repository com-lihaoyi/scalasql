package scalasql.query

import scalasql.renderer.SqlStr.SqlStringSyntax
import scalasql.renderer.{Context, ExprsToSql, SelectToSql, SqlStr, UpdateToSql}
import scalasql.Queryable
import scalasql.utils.OptionPickler

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
) extends Query[Seq[R]] {
  def valueReader: OptionPickler.Reader[Seq[R]] =
    OptionPickler.SeqLikeReader(qr.valueReader(returning), implicitly)

  def walk() = qr.walk(returning)

  override def singleRow = false

  override def toSqlQuery(implicit ctx0: Context): SqlStr = toSqlQuery0(ctx0)
  def toSqlQuery0(ctx0: Context): SqlStr = {
    implicit val (_, _, _, ctx) = SelectToSql.computeContext(ctx0, Nil, Some(returnable.table))

    val prefix = returnable.toSqlQuery
    val (flattenedExpr, exprStr) = ExprsToSql.apply0(qr.walk(returning), ctx, sql"")
    val suffix = sql" RETURNING $exprStr"

    prefix + suffix
  }
}
