package usql.query

import usql.renderer.SqlStr.SqlStringSyntax
import usql.renderer.{Context, Interp, SelectToSql, SqlStr}
import usql.Queryable
import usql.utils.OptionPickler

trait Select[Q] extends Interp.Renderable with Aggregatable[Q] with From with Joinable[Q]
    with JoinOps[Select, Q] with Query{

  protected def qr: Queryable[Q, _]
  def isTrivialJoin: Boolean = false
  def select = this

  def distinct: Select[Q]

  def simple(args: Iterable[_]*) = args.forall(_.isEmpty)

  def subquery(implicit qr: Queryable[Q, _]) = new SubqueryRef[Q](this, qr)

  def map[V](f: Q => V)(implicit qr: Queryable[V, _]): Select[V]
  def flatMap[V](f: Q => Select[V])(implicit qr: Queryable[V, _]): Select[V]
  def filter(f: Q => Expr[Boolean]): Select[Q]

  def aggregate[E, V](f: SelectProxy[Q] => E)(implicit qr: Queryable[E, V]): Expr[V]

  def groupBy[K, V](groupKey: Q => K)(groupAggregate: SelectProxy[Q] => V)(implicit
      qrk: Queryable[K, _],
      qrv: Queryable[V, _]
  ): Select[(K, V)]

  def sortBy(f: Q => Expr[_]): Select[Q]
  def asc: Select[Q]
  def desc: Select[Q]
  def nullsFirst: Select[Q]
  def nullsLast: Select[Q]

  def union(other: Select[Q]): Select[Q] = compound0("UNION", other)
  def unionAll(other: Select[Q]): Select[Q] = compound0("UNION ALL", other)
  def intersect(other: Select[Q]): Select[Q] = compound0("INTERSECT", other)
  def except(other: Select[Q]): Select[Q] = compound0("EXCEPT", other)
  def compound0(op: String, other: Select[Q]): CompoundSelect[Q]

  def drop(n: Int): Select[Q]
  def take(n: Int): Select[Q]



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
  def fromTable[T](e: T, table: TableRef)(implicit qr: Queryable[T, _]) = {
    SimpleSelect(e, None, Seq(table), Nil, Nil, None)
  }

  implicit def SelectQueryable[Q, R](implicit qr: Queryable[Q, R]): Queryable[Select[Q], Seq[R]] =
    new Query.Queryable[Select[Q], Seq[R]]()(OptionPickler.SeqLikeReader(qr.valueReader, implicitly))
}
