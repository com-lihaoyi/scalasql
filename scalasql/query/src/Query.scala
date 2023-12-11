package scalasql.query

import scalasql.core.SqlStr.Renderable
import scalasql.core.{Context, Expr, Queryable, SqlStr, WithSqlExpr}

/**
 * A SQL Query, either a [[Query.Multiple]] that returns multiple rows, or
 * a [[Query.Single]] that returns a single row
 */
trait Query[R] extends Renderable {
  protected def queryWalkLabels(): Seq[List[String]]
  protected def queryWalkExprs(): Seq[Expr[_]]
  protected def queryIsSingleRow: Boolean
  protected def queryIsExecuteUpdate: Boolean = false

  protected def queryConstruct(args: Queryable.ResultSetIterator): R
}

object Query {
  trait ExecuteUpdate[R] extends scalasql.query.Query[R] {
    protected def queryWalkLabels() = Nil
    protected def queryWalkExprs() = Nil
    protected override def queryIsSingleRow = true
    protected override def queryIsExecuteUpdate = true
  }

  trait DelegateQuery[R] extends scalasql.query.Query[R] {
    protected def queryDelegate: Query[_]
    protected def queryWalkLabels() = queryDelegate.queryWalkLabels()
    protected def queryWalkExprs() = queryDelegate.queryWalkExprs()
    protected override def queryIsSingleRow = queryDelegate.queryIsSingleRow
    protected override def queryIsExecuteUpdate = queryDelegate.queryIsExecuteUpdate
  }

  trait DelegateQueryable[Q, R] extends scalasql.query.Query[R] with WithSqlExpr[Q] {
    protected def qr: Queryable[Q, _]
    protected def queryWalkLabels() = qr.walkLabels(expr)
    protected def queryWalkExprs() = qr.walkExprs(expr)
    protected override def queryIsSingleRow = qr.isSingleRow(expr)
    protected override def queryIsExecuteUpdate = qr.isExecuteUpdate(expr)
  }

  implicit def QueryQueryable[R]: Queryable[Query[R], R] = new QueryQueryable[Query[R], R]()

  def walkLabels[R](q: Query[R]) = q.queryWalkLabels()
  def walkSqlExprs[R](q: Query[R]) = q.queryWalkExprs()
  def isSingleRow[R](q: Query[R]) = q.queryIsSingleRow
  def construct[R](q: Query[R], args: Queryable.ResultSetIterator) = q.queryConstruct(args)
  class QueryQueryable[Q <: Query[R], R]() extends scalasql.core.Queryable[Q, R] {
    override def isExecuteUpdate(q: Q) = q.queryIsExecuteUpdate
    override def walkLabels(q: Q) = q.queryWalkLabels()
    override def walkExprs(q: Q) = q.queryWalkExprs()
    override def isSingleRow(q: Q) = q.queryIsSingleRow

    def toSqlStr(q: Q, ctx: Context): SqlStr = q.renderSql(ctx)

    override def construct(q: Q, args: Queryable.ResultSetIterator): R = q.queryConstruct(args)
  }

  class Single[R](query: Query[Seq[R]]) extends Query.DelegateQuery[R] {
    protected def queryDelegate = query
    protected override def queryIsSingleRow: Boolean = true

    protected def renderSql(ctx: Context): SqlStr = Renderable.renderSql(query)(ctx)
    protected override def queryConstruct(args: Queryable.ResultSetIterator): R =
      query.queryConstruct(args).asInstanceOf[R]
  }
}
