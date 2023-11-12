package scalasql.query

import scalasql.renderer.SqlStr.{Renderable, SqlStringSyntax}
import scalasql.renderer.{Context, ExprsToSql, JoinsToSql, SqlStr}
import scalasql.{TypeMapper, Queryable}
import scalasql.utils.OptionPickler

/**
 * A query that could support a `RETURNING` clause, typically
 * an `INSERT` or `UPDATE`
 */
trait Returnable[Q] extends Renderable with WithExpr[Q] {
  def table: TableRef
}

trait InsertReturnable[Q] extends Returnable[Q]

/**
 * A query with a `RETURNING` clause
 */
trait Returning[Q, R] extends Query.Multiple[R] {
  def single: Query.Single[R] = new Query.Single(this)
}

trait InsertReturning[Q, R] extends Returning[Q, R]
object InsertReturning {
  class Impl[Q, R](returnable: InsertReturnable[_], returning: Q)(
      implicit val qr: Queryable.Row[Q, R]
  ) extends Returning.Impl0[Q, R](qr, returnable, returning)
      with InsertReturning[Q, R] {
    protected def expr: Q = returning
  }
}
object Returning {
  class Impl0[Q, R](qr: Queryable.Row[Q, R], returnable: Returnable[_], returning: Q)
      extends Returning[Q, R] {
    protected def queryValueReader =
      OptionPickler.SeqLikeReader2(qr.valueReader(returning), implicitly)

    def queryWalkExprs() = qr.walk(returning)

    override def queryIsSingleRow = false

    override def renderToSql(implicit ctx0: Context): (SqlStr, Seq[TypeMapper[_]]) =
      toSqlQuery0(ctx0)

    def toSqlQuery0(ctx0: Context): (SqlStr, Seq[TypeMapper[_]]) = {
      val computed = Context.compute(ctx0, Nil, Some(returnable.table))
      import computed.implicitCtx

      val (prefix, prevExprs) = Renderable.renderToSql(returnable)
      val flattenedExpr = qr.walk(returning)
      val exprStr = ExprsToSql.apply0(flattenedExpr, implicitCtx, sql"")
      val suffix = sql" RETURNING $exprStr"

      (prefix + suffix, flattenedExpr.map(t => Expr.getMappedType(t._2)))
    }
  }
  class Impl[Q, R](returnable: Returnable[_], returning: Q)(implicit val qr: Queryable.Row[Q, R])
      extends Impl0[Q, R](qr, returnable, returning)
      with Returning[Q, R]

}
