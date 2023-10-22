package usql.query

import usql.Queryable
import usql.renderer.{Context, SqlStr}
import usql.utils.OptionPickler

trait Query[R] {
  def walk(): Seq[(List[String], Expr[_])]
  def singleRow: Boolean
  def toSqlQuery(implicit ctx: Context): SqlStr
  def valueReader: OptionPickler.Reader[R]
  def isExecuteUpdate: Boolean = false
}

object Query {


  class Queryable[Q <: Query[R], R]() extends usql.Queryable[Q, R] {
    override def isExecuteUpdate(q: Q) = q.isExecuteUpdate
    override def walk(q: Q) = q.walk()
    override def singleRow(q: Q) = q.singleRow

    override def valueReader(q: Q): OptionPickler.Reader[R] = q.valueReader
    override def toSqlQuery(q: Q, ctx: Context): SqlStr = q.toSqlQuery(ctx)
  }
}
