package scalasql.query

import scalasql.renderer.SqlStr.Renderable
import scalasql.{Queryable, TypeMapper}
import scalasql.renderer.{Context, SqlStr}

/**
 * A SQL Query, either a [[Query.Multiple]] that returns multiple rows, or
 * a [[Query.Single]] that returns a single row
 */
trait Query[R] extends Renderable {
  protected def queryWalkExprs(): Seq[(List[String], Expr[_])]
  protected def queryIsSingleRow: Boolean
  protected def queryIsExecuteUpdate: Boolean = false

  protected def queryConstruct(args: Queryable.ResultSetIterator): R
}

object Query {
  def queryWalkExprs[R](q: Query[R]) = q.queryWalkExprs()
  def queryIsSingleRow[R](q: Query[R]) = q.queryIsSingleRow
  def queryConstruct[R](q: Query[R], args: Queryable.ResultSetIterator) = q.queryConstruct(args)
  class Queryable[Q <: Query[R], R]() extends scalasql.Queryable[Q, R] {
    override def isExecuteUpdate(q: Q) = q.queryIsExecuteUpdate
    override def walkLabels(q: Q) = q.queryWalkExprs().map(_._1)
    override def walkExprs(q: Q) = q.queryWalkExprs().map(_._2)
    override def singleRow(q: Q) = q.queryIsSingleRow

    def toSqlStr(q: Q, ctx: Context): SqlStr = q.renderToSql(ctx)

    override def construct(q: Q, args: Queryable.ResultSetIterator): R = q.queryConstruct(args)
  }

  trait Multiple[R] extends Query[Seq[R]]

  class Single[R](query: Multiple[R]) extends Query[R] {
    override def queryIsExecuteUpdate = query.queryIsExecuteUpdate
    protected def queryWalkExprs() = query.queryWalkExprs()

    protected def queryIsSingleRow: Boolean = true

    protected def renderToSql(ctx: Context): SqlStr = Renderable.renderToSql(query)(ctx)
    protected override def queryConstruct(args: Queryable.ResultSetIterator): R =
      query.queryConstruct(args).asInstanceOf[R]
  }
}
