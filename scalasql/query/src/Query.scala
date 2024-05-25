package scalasql.query

import scalasql.core.SqlStr.Renderable
import scalasql.core.{Context, Expr, Queryable, SqlStr, WithSqlExpr}

/**
 * A SQL Query, either a [[Query.Multiple]] that returns multiple rows, or
 * a [[Query.Single]] that returns a single row
 */
trait Query[R] extends Renderable {
  protected def queryWalkLabels(): Seq[List[String]]
  protected def queryWalkExprs(): Seq[Expr[?]]
  protected def queryIsSingleRow: Boolean
  protected def queryGetGeneratedKeys: Option[Queryable.Row[?, ?]] = None
  protected def queryIsExecuteUpdate: Boolean = false

  protected def queryConstruct(args: Queryable.ResultSetIterator): R
}

object Query {

  /**
   * Configuration for a typical update [[Query]]
   */
  trait ExecuteUpdate[R] extends scalasql.query.Query[R] {
    protected def queryWalkLabels(): Seq[List[String]] = Nil
    protected def queryWalkExprs(): Seq[Expr[?]] = Nil
    protected override def queryIsSingleRow = true
    protected override def queryIsExecuteUpdate = true
  }

  /**
   * Configuration for a [[Query]] that wraps another [[Query]], delegating
   * most of the abstract methods to it
   */
  trait DelegateQuery[R] extends scalasql.query.Query[R] {
    protected def query: Query[?]
    protected def queryWalkLabels() = query.queryWalkLabels()
    protected def queryWalkExprs() = query.queryWalkExprs()
    protected override def queryIsSingleRow = query.queryIsSingleRow
    protected override def queryIsExecuteUpdate = query.queryIsExecuteUpdate
  }

  /**
   * Configuration for a [[Query]] that wraps an expr [[Q]] and [[Queryable]]
   */
  trait DelegateQueryable[Q, R] extends scalasql.query.Query[R] with WithSqlExpr[Q] {
    protected def qr: Queryable[Q, ?]
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

  /**
   * The default [[Queryable]] instance for any [[Query]]. Delegates the implementation
   * of the [[Queryable]] methods to abstract methods on the [[Query]], to allow easy
   * overrides and subclassing of [[Query]] classes
   */
  class QueryQueryable[Q <: Query[R], R]() extends scalasql.core.Queryable[Q, R] {
    override def isGetGeneratedKeys(q: Q) = q.queryGetGeneratedKeys
    override def isExecuteUpdate(q: Q) = q.queryIsExecuteUpdate
    override def walkLabels(q: Q) = q.queryWalkLabels()
    override def walkExprs(q: Q) = q.queryWalkExprs()
    override def isSingleRow(q: Q) = q.queryIsSingleRow

    def renderSql(q: Q, ctx: Context): SqlStr = q.renderSql(ctx)

    override def construct(q: Q, args: Queryable.ResultSetIterator): R = q.queryConstruct(args)
  }

  /**
   * A [[Query]] that wraps another [[Query]] but sets [[queryIsSingleRow]] to `true`
   */
  class Single[R](protected val query: Query[Seq[R]]) extends Query.DelegateQuery[R] {
    protected override def queryIsSingleRow: Boolean = true

    private[scalasql] def renderSql(ctx: Context): SqlStr = Renderable.renderSql(query)(ctx)
    protected override def queryConstruct(args: Queryable.ResultSetIterator): R =
      query.queryConstruct(args).asInstanceOf[R]
  }
}
