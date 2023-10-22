package usql.query

import usql.renderer.SqlStr.SqlStringSyntax
import usql.renderer.{Context, Interp, SelectToSql, SqlStr}
import usql.Queryable
import usql.utils.OptionPickler

trait Select[Q, R]
    extends Interp.Renderable
    with Aggregatable[Q]
    with From
    with Joinable[Q, R]
    with JoinOps[Select, Q, R]
    with Query[Seq[R]] {

  def qr: Queryable[Q, R]
  def isTrivialJoin: Boolean = false
  def select = this

  def distinct: Select[Q, R]

  def simple(args: Iterable[_]*) = args.forall(_.isEmpty)

  def subquery(implicit qr: Queryable[Q, R]) = new SubqueryRef[Q, R](this, qr)

  def map[Q2, R2](f: Q => Q2)(implicit qr: Queryable[Q2, R2]): Select[Q2, R2]
  def flatMap[Q2, R2](f: Q => Select[Q2, R2])(implicit qr: Queryable[Q2, R2]): Select[Q2, R2]
  def filter(f: Q => Expr[Boolean]): Select[Q, R]

  def aggregate[E, V](f: SelectProxy[Q] => E)(implicit qr: Queryable[E, V]): Expr[V]

  def groupBy[K, V, R2, R3](groupKey: Q => K)(groupAggregate: SelectProxy[Q] => V)(implicit
      qrk: Queryable[K, R2],
      qrv: Queryable[V, R3]
  ): Select[(K, V), (R2, R3)]

  def sortBy(f: Q => Expr[_]): Select[Q, R]
  def asc: Select[Q, R]
  def desc: Select[Q, R]
  def nullsFirst: Select[Q, R]
  def nullsLast: Select[Q, R]

  def union(other: Select[Q, R]): Select[Q, R] = compound0("UNION", other)
  def unionAll(other: Select[Q, R]): Select[Q, R] = compound0("UNION ALL", other)
  def intersect(other: Select[Q, R]): Select[Q, R] = compound0("INTERSECT", other)
  def except(other: Select[Q, R]): Select[Q, R] = compound0("EXCEPT", other)
  def compound0(op: String, other: Select[Q, R]): CompoundSelect[Q, R]

  def drop(n: Int): Select[Q, R]
  def take(n: Int): Select[Q, R]

  def toSqlQuery(implicit ctx: Context): SqlStr = {
    SelectToSql.apply(
      this,
      qr,
      ctx.tableNameMapper,
      ctx.columnNameMapper,
      ctx.fromNaming
    )._2.withCompleteQuery(true)
  }
  def walk() = qr.walk(expr)
  override def singleRow = false
}

object Select {
  def fromTable[T, R](e: T, table: TableRef)(implicit qr: Queryable[T, R]) = {
    SimpleSelect(e, None, Seq(table), Nil, Nil, None)
  }

  implicit def SelectQueryable[Q, R]: Queryable[Select[Q, R], Seq[R]] = Queryable.QueryQueryable
}
