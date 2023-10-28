package scalasql.query

import scalasql.{MappedType, Queryable}
import scalasql.renderer.{Context, SqlStr}
import scalasql.utils.OptionPickler

trait Query[R] {
  def walk(): Seq[(List[String], Expr[_])]
  def singleRow: Boolean
  def toSqlQuery(implicit ctx: Context): (SqlStr, Seq[MappedType[_]])
  def valueReader: OptionPickler.Reader[R]
  def isExecuteUpdate: Boolean = false

}

object Query {

  class Queryable[Q <: Query[R], R]() extends scalasql.Queryable[Q, R] {
    override def isExecuteUpdate(q: Q) = q.isExecuteUpdate
    override def walk(q: Q) = q.walk()
    override def singleRow(q: Q) = q.singleRow

    override def valueReader(q: Q): OptionPickler.Reader[R] = q.valueReader
    override def toSqlQuery(q: Q, ctx: Context): (SqlStr, Seq[MappedType[_]]) = q.toSqlQuery(ctx)
  }

  trait Multiple[R] extends Query[Seq[R]] {
    def valueReader: OptionPickler.SeqLikeReader[Seq, R]
  }

  class Single[R](query: Multiple[R]) extends Query[R] {
    override def isExecuteUpdate = query.isExecuteUpdate
    def walk() = query.walk()

    def singleRow: Boolean = true

    def toSqlQuery(implicit ctx: Context) = query.toSqlQuery

    def valueReader = query.valueReader.r
  }
}
