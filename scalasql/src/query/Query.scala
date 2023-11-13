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

  protected override def renderToSql(implicit ctx: Context): SqlStr = toSqlStr(ctx)
  def toSqlStr(ctx: Context): SqlStr
  def toTypeMappers(): Seq[TypeMapper[_]]
}

object Query {

  def getWalkExprs[R](q: Query[R]) = q.queryWalkExprs()
  def getIsSingleRow[R](q: Query[R]) = q.queryIsSingleRow
  def getValueReader[R](q: Query[R]) = q.queryValueReader
  class Queryable[Q <: Query[R], R]() extends scalasql.Queryable[Q, R] {
    override def isExecuteUpdate(q: Q) = q.queryIsExecuteUpdate
    override def walk(q: Q) = q.queryWalkExprs()
    override def singleRow(q: Q) = q.queryIsSingleRow

    override def valueReader(q: Q): OptionPickler.Reader[R] = q.queryValueReader
    def toSqlStr(q: Q, ctx: Context): SqlStr = q.toSqlStr(ctx)
    def toTypeMappers(q: Q): Seq[TypeMapper[_]] = q.toTypeMappers()
  }

  trait Multiple[R] extends Query[Seq[R]]

  class Single[R](query: Multiple[R]) extends Query[R] {
    override def queryIsExecuteUpdate = query.queryIsExecuteUpdate
    protected def queryWalkExprs() = query.queryWalkExprs()

    protected def queryIsSingleRow: Boolean = true

    def toSqlStr(ctx: Context): SqlStr = query.toSqlStr(ctx)

    def toTypeMappers(): Seq[TypeMapper[_]] = query.toTypeMappers()

    protected def queryValueReader =
      Query.getValueReader(query).asInstanceOf[OptionPickler.SeqLikeReader2[Seq, R]].r
  }
}
