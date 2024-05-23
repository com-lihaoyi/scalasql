package scalasql.query

import scalasql.core.SqlStr.Renderable
import scalasql.core.{Context, Expr, Queryable, SqlStr, WithSqlExpr}

/**
 * Represents an [[Insert]] query that you want to call `JdbcStatement.getGeneratedKeys`
 * on to retrieve any auto-generated primary key values from the results
 */
trait GetGeneratedKeys[Q, R] extends Query[Seq[R]] {
  def single: Query.Single[R] = new Query.Single(this)
}

object GetGeneratedKeys {

  class Impl[Q, R](base: Returning.InsertBase[Q])(implicit qr: Queryable.Row[?, R])
      extends GetGeneratedKeys[Q, R] {

    def expr = WithSqlExpr.get(base)
    override protected def queryConstruct(args: Queryable.ResultSetIterator): Seq[R] = {
      Seq(qr.construct(args))
    }

    protected def queryWalkLabels(): Seq[List[String]] = Nil
    protected def queryWalkExprs(): Seq[Expr[?]] = Nil
    protected override def queryIsSingleRow = false
    protected override def queryIsExecuteUpdate = true

    override private[scalasql] def renderSql(ctx: Context): SqlStr = Renderable.renderSql(base)(ctx)

    override protected def queryGetGeneratedKeys: Option[Queryable.Row[?, ?]] = Some(qr)
  }
}
