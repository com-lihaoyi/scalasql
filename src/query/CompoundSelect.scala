package usql.query

import usql.renderer.SqlStr.SqlStringSyntax
import usql.renderer.{Context, SqlStr}
import usql.{Queryable, Table}



case class CompoundSelect[Q](lhs: SimpleSelect[Q],
                             compoundOps: Seq[CompoundSelect.Op[Q]],
                             orderBy: Option[OrderBy],
                             limit: Option[Int],
                             offset: Option[Int])
                            (implicit val qr: Queryable[Q, _]) extends Select[Q] {

  def expr = lhs.select.expr

  override def select = this

  def distinct: Select[Q] = ???

  def queryExpr[V](f: Q => Context => SqlStr)
                  (implicit qr: Queryable[Expr[V], V]): Expr[V] = ???

  def map[V](f: Q => V)(implicit qr2: Queryable[V, _]): Select[V] = {
    (lhs, compoundOps) match {
      case (s: Select[Q], Nil) =>
        CompoundSelect(SimpleSelect.from(s.map(f)), Nil, orderBy, limit, offset)

      case _ => SimpleSelect.from(this).map(f)
    }
  }

  def flatMap[V](f: Q => Select[V])(implicit qr: Queryable[V, _]): Select[V] = {
    ???
  }

  def filter(f: Q => Expr[Boolean]): Select[Q] = {
    (lhs, compoundOps) match {
      case (s: SimpleSelect[Q], Nil) => CompoundSelect(SimpleSelect.from(s.filter(f)), compoundOps, orderBy, limit, offset)
      case _ => SimpleSelect.from(this).filter(f)
    }
  }

  def join0[V](other: Joinable[V],
               on: Option[(Q, V) => Expr[Boolean]])
              (implicit joinQr: Queryable[V, _]): Select[(Q, V)] = {
    SimpleSelect.from(this).join0(other, on)
  }

  def aggregate[E, V](f: SelectProxy[Q] => E)
                     (implicit qr: Queryable[E, V]): Expr[V] = {
    SimpleSelect.from(this).aggregate(f)
  }

  def groupBy[K, V](groupKey: Q => K)
                   (groupAggregate: SelectProxy[Q] => V)
                   (implicit qrk: Queryable[K, _], qrv: Queryable[V, _]): Select[(K, V)] = {
    SimpleSelect.from(this).groupBy(groupKey)(groupAggregate)
  }

  def sortBy(f: Q => Expr[_]) = {
    val newOrder = Some(OrderBy(f(expr), None, None))

    if (simple(limit, offset)) copy(orderBy = newOrder)
    else CompoundSelect(SimpleSelect.from(this), compoundOps, newOrder, None, None)
  }

  def asc = copy(orderBy = Some(orderBy.get.copy(ascDesc = Some(AscDesc.Asc))))
  def desc = copy(orderBy = Some(orderBy.get.copy(ascDesc = Some(AscDesc.Desc))))
  def nullsFirst = copy(orderBy = Some(orderBy.get.copy(nulls = Some(Nulls.First))))
  def nullsLast = copy(orderBy = Some(orderBy.get.copy(nulls = Some(Nulls.Last))))

  def compound0(op: String, other: Select[Q]) = {
    val op2 = CompoundSelect.Op(op, SimpleSelect.from(other))
    if (simple(orderBy, limit, offset)) copy(compoundOps = compoundOps ++ Seq(op2))
    else CompoundSelect(SimpleSelect.from(this), Seq(op2), None, None, None)
  }

  def drop(n: Int) = copy(offset = Some(offset.getOrElse(0) + n), limit = limit.map(_ - n))
  def take(n: Int) = copy(limit = Some(limit.fold(n)(math.min(_, n))))

  override def toSqlExpr0(implicit ctx: Context): SqlStr = {
    Select.SelectQueryable(qr).toSqlQuery(this, ctx).asCompleteQuery
  }
}

object CompoundSelect {
  case class Op[Q](op: String, rhs: SimpleSelect[Q])
}