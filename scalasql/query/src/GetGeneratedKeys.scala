package scalasql.query

import scalasql.core.SqlStr.Renderable
import scalasql.core.{Context, Queryable, SqlStr, WithSqlExpr}

/**
 * Represents an [[Insert]] query that you want to call `JdbcStatement.getGeneratedKeys`
 * on to retrieve any auto-generated primary key values from the results
 */
trait GetGeneratedKeys[Q, R] extends Query[Seq[R]] {
  def single: Query.Single[R] = new Query.Single(this)
}

object GetGeneratedKeys {

  class Impl[Q, R](base: Returning.InsertBase[Q])(implicit qr: Queryable.Row[_, R])
      extends GetGeneratedKeys[Q, R] {

    def expr = WithSqlExpr.get(base)
    override protected def queryConstruct(args: Queryable.ResultSetIterator): Seq[R] = {
      Seq(qr.construct(args))
    }

    protected def queryWalkLabels() = Nil
    protected def queryWalkExprs() = Nil
    protected override def queryIsSingleRow = false
    protected override def queryIsExecuteUpdate = true

    override protected def renderSql(ctx: Context): SqlStr = Renderable.renderSql(base)(ctx)

    override protected def queryGetGeneratedKeys: Option[Queryable.Row[_, _]] = Some(qr)
  }
}
