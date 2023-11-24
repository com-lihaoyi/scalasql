package scalasql.query

import scalasql.renderer.SqlStr.Renderable
import scalasql.{TypeMapper, Queryable}
import scalasql.renderer.{Context, SqlStr}
import scalasql.utils.OptionPickler

/**
 * A SQL Query, either a [[Query.Multiple]] that returns multiple rows, or
 * a [[Query.Single]] that returns a single row
 */
trait Query[R] extends Renderable {
  protected def queryWalkExprs(): Seq[(List[String], Expr[_])]
  protected def queryIsSingleRow: Boolean
  protected def queryValueReader: OptionPickler.Reader[R]
  protected def queryIsExecuteUpdate: Boolean = false

  protected def queryTypeMappers(): Seq[TypeMapper[_]]
}

object Query {
  def queryTypeMappers[R](q: Query[R]) = q.queryTypeMappers()
  def queryWalkExprs[R](q: Query[R]) = q.queryWalkExprs()
  def queryIsSingleRow[R](q: Query[R]) = q.queryIsSingleRow
  def queryValueReader[R](q: Query[R]) = q.queryValueReader
  class Queryable[Q <: Query[R], R]() extends scalasql.Queryable[Q, R] {
    override def isExecuteUpdate(q: Q) = q.queryIsExecuteUpdate
    override def walkLabels(q: Q) = q.queryWalkExprs().map(_._1)
    override def walkExprs(q: Q) = q.queryWalkExprs().map(_._2)
    override def singleRow(q: Q) = q.queryIsSingleRow

    override def valueReader(q: Q): OptionPickler.Reader[R] = q.queryValueReader
    def toSqlStr(q: Q, ctx: Context): SqlStr = q.renderToSql(ctx)
    def toTypeMappers(q: Q): Seq[TypeMapper[_]] = q.queryTypeMappers()
  }

  trait Multiple[R] extends Query[Seq[R]]

  class Single[R](query: Multiple[R]) extends Query[R] {
    override def queryIsExecuteUpdate = query.queryIsExecuteUpdate
    protected def queryWalkExprs() = query.queryWalkExprs()

    protected def queryIsSingleRow: Boolean = true

    protected def renderToSql(ctx: Context): SqlStr = Renderable.renderToSql(query)(ctx)

    protected def queryTypeMappers(): Seq[TypeMapper[_]] = query.queryTypeMappers()

    protected def queryValueReader =
      Query.queryValueReader(query).asInstanceOf[OptionPickler.SeqLikeReader2[Seq, R]].r
  }
}
