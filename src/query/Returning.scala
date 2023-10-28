package scalasql.query

import scalasql.renderer.SqlStr.SqlStringSyntax
import scalasql.renderer.{Context, ExprsToSql, SelectToSql, SqlStr}
import scalasql.{MappedType, Queryable}
import scalasql.utils.OptionPickler

trait Returnable[Q] {
  def expr: Q
  def table: TableRef
  def toSqlQuery(implicit ctx: Context): (SqlStr, Seq[MappedType[_]])
}

trait InsertReturnable[Q] extends Returnable[Q]

trait Returning[Q, R] extends Query.Multiple[R] {
  def single: Query.Single[R] = new Query.Single(this)
}

trait InsertReturning[Q, R] extends Returning[Q, R]
object InsertReturning {
  case class Impl[Q, R](returnable: InsertReturnable[_], returning: Q)(
      implicit val qr: Queryable[Q, R]
  ) extends Returning.Impl0[Q, R](qr, returnable, returning) with InsertReturning[Q, R] {
    def expr: Q = returning
  }
}
object Returning {
  class Impl0[Q, R](qr: Queryable[Q, R], returnable: Returnable[_], returning: Q)
      extends Returning[Q, R] {
    def valueReader = OptionPickler.SeqLikeReader(qr.valueReader(returning), implicitly)

    def walk() = qr.walk(returning)

    override def singleRow = false

    override def toSqlQuery(implicit ctx0: Context): (SqlStr, Seq[MappedType[_]]) =
      toSqlQuery0(ctx0)

    def toSqlQuery0(ctx0: Context): (SqlStr, Seq[MappedType[_]]) = {
      val computed = Context.compute(ctx0, Nil, Some(returnable.table))
      import computed.implicitCtx

      val (prefix, prevExprs) = returnable.toSqlQuery
      val (flattenedExpr, exprStr) = ExprsToSql.apply0(qr.walk(returning), implicitCtx, sql"")
      val suffix = sql" RETURNING $exprStr"

      (prefix + suffix, flattenedExpr.map(t => Expr.getMappedType(t._2)))
    }
  }
  case class Impl[Q, R](returnable: Returnable[_], returning: Q)(implicit val qr: Queryable[Q, R])
      extends Impl0[Q, R](qr, returnable, returning) with Returning[Q, R]

}
