package scalasql.query

import scalasql.renderer.SqlStr.Renderable
import scalasql.{MappedType, Queryable}
import scalasql.renderer.{Context, SqlStr}
import scalasql.utils.OptionPickler

/**
 * A SQL Query, either a [[Query.Multiple]] that returns multiple rows, or
 * a [[Query.Single]] that returns a single row
 */
trait Query[R] extends Renderable {
  def queryWalkExprs(): Seq[(List[String], Expr[_])]
  def queryIsSingleRow: Boolean
  def queryValueReader: OptionPickler.Reader[R]
  def queryIsExecuteUpdate: Boolean = false
}

object Query {

  class Queryable[Q <: Query[R], R]() extends scalasql.Queryable[Q, R] {
    override def isExecuteUpdate(q: Q) = q.queryIsExecuteUpdate
    override def walk(q: Q) = q.queryWalkExprs()
    override def singleRow(q: Q) = q.queryIsSingleRow

    override def valueReader(q: Q): OptionPickler.Reader[R] = q.queryValueReader
    override def toSqlQuery(q: Q, ctx: Context): (SqlStr, Seq[MappedType[_]]) = q.renderToSql(ctx)
  }

  trait Multiple[R] extends Query[Seq[R]] {
    def queryValueReader: OptionPickler.SeqLikeReader[Seq, R]
  }

  class Single[R](query: Multiple[R]) extends Query[R] {
    override def queryIsExecuteUpdate = query.queryIsExecuteUpdate
    def queryWalkExprs() = query.queryWalkExprs()

    def queryIsSingleRow: Boolean = true

    def renderToSql(implicit ctx: Context) = query.renderToSql

    def queryValueReader = query.queryValueReader.r
  }
}
