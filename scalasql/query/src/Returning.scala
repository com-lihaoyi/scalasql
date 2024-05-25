package scalasql.query

import scalasql.core.SqlStr.{Renderable, SqlStringSyntax}
import scalasql.core.{Context, ExprsToSql, Queryable, SqlStr, WithSqlExpr}

/**
 * A query with a `RETURNING` clause
 */
trait Returning[Q, R] extends Query[Seq[R]] with Query.DelegateQueryable[Q, Seq[R]] {
  def single: Query.Single[R] = new Query.Single(this)
}

object Returning {

  /**
   * A query that could support a `RETURNING` clause, typically
   * an `INSERT` or `UPDATE`
   */
  trait Base[Q] extends Renderable with WithSqlExpr[Q] {
    def table: TableRef
  }

  trait InsertBase[Q] extends Base[Q] {

    /**
     * Makes this `INSERT` query call `JdbcStatement.getGeneratedKeys` when it is executed,
     * returning a `Seq[R]` where `R` is a Scala type compatible with the auto-generated
     * primary key type (typically something like `Int` or `Long`)
     */
    def getGeneratedKeys[R](implicit qr: Queryable.Row[?, R]): GetGeneratedKeys[Q, R] = {
      new GetGeneratedKeys.Impl(this)
    }
  }

  class InsertImpl[Q, R](returnable: InsertBase[?], returning: Q)(
      implicit qr: Queryable.Row[Q, R]
  ) extends Returning.Impl0[Q, R](qr, returnable, returning)
      with Returning[Q, R] {}

  class Impl[Q, R](returnable: Base[?], returning: Q)(implicit qr: Queryable.Row[Q, R])
      extends Impl0[Q, R](qr, returnable, returning)
      with Returning[Q, R]

  class Impl0[Q, R](
      protected val qr: Queryable.Row[Q, R],
      returnable: Base[?],
      protected val expr: Q
  ) extends Returning[Q, R] {

    override protected def queryConstruct(args: Queryable.ResultSetIterator): Seq[R] = {
      Seq(qr.construct(args))
    }

    override def queryIsSingleRow = false

    private[scalasql] override def renderSql(ctx0: Context) = {
      implicit val implicitCtx = Context.compute(ctx0, Nil, Some(returnable.table))

      val prefix = Renderable.renderSql(returnable)
      val walked = qr.walkLabelsAndExprs(expr)
      val exprStr = ExprsToSql.apply(walked, implicitCtx, SqlStr.empty)
      val suffix = sql" RETURNING $exprStr"

      prefix + suffix
    }

  }

}
