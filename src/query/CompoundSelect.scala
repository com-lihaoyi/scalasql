package usql.query

import usql.renderer.SqlStr.SqlStringSyntax
import usql.renderer.{Context, SqlStr}
import usql.{Queryable, Table}



case class CompoundSelect[Q](lhs: SimpleSelect[Q],
                             compoundOps: Seq[CompoundSelect.Op],
                             orderBy: Option[OrderBy],
                             limit: Option[Int],
                             offset: Option[Int])
                            (implicit val qr: Queryable[Q, _]) extends Select[Q] {

  def expr = lhs.select.expr

  override def select = this

  def distinct: Select[Q] = ???

  def queryExpr[V](f: Q => Context => SqlStr)
                  (implicit qr: Queryable[Expr[V], V]): Expr[V] = {
    ???
//    Expr[V] { implicit outerCtx: Context =>
//      this.copy[Expr[V]](expr = Expr[V] { implicit ctx: Context =>
//        val newCtx = new Context(
//          outerCtx.fromNaming ++ ctx.fromNaming,
//          ctx.exprNaming,
//          ctx.tableNameMapper,
//          ctx.columnNameMapper
//        )
//
//        f(newCtx)
//      }).toSqlExpr.asCompleteQuery
//    }
  }

  def map[V](f: Q => V)(implicit qr2: Queryable[V, _]): Select[V] = {
    (lhs, compoundOps) match {
      case (s: SimpleSelect[Q], Nil) => CompoundSelect(SimpleSelect.from(s.map(f)), compoundOps, orderBy, limit, offset)

      case (cs: CompoundSelect[Q], Nil) =>
        val ref = new SubqueryRef(cs, cs.qr)
        this.copy(lhs = SimpleSelect(cs.lhs.asInstanceOf[Select[Q]].map(f).expr, None, Seq(ref), Nil, Nil, None))

      case _ => SimpleSelect(f(expr), None, Seq(this.subquery), Nil, Nil, None)
    }
  }

  def flatMap[V](f: Q => Select[V])(implicit qr: Queryable[V, _]): Select[V] = {
    ???
  }

  def filter(f: Q => Expr[Boolean]): Select[Q] = {
    (lhs, compoundOps) match {
      case (s: SimpleSelect[Q], Nil) => CompoundSelect(SimpleSelect.from(s.filter(f)), compoundOps, orderBy, limit, offset)
      case _ => SimpleSelect(expr, None, Seq(this.subquery), Nil, Seq(f(expr)), None)
    }
  }

  def join0[V](other: Joinable[V],
               on: Option[(Q, V) => Expr[Boolean]])
              (implicit joinQr: Queryable[V, _]): Select[(Q, V)] = {

    val otherTrivial = other.isInstanceOf[Table.Base]

    val otherSelect = other.select
    lazy val otherTableJoin = Join(None, Seq(JoinFrom(otherSelect.asInstanceOf[SimpleSelect[_]].from.head, on.map(_(expr, otherSelect.expr)))))
    lazy val otherSubqueryJoin = Join(None, Seq(JoinFrom(new SubqueryRef(otherSelect, joinQr), on.map(_(expr, otherSelect.expr)))))
    SimpleSelect(
      expr = (expr, otherSelect.expr),
      exprPrefix = None,
      from = Seq(this.subquery),
      joins = if (otherTrivial) Seq(otherTableJoin) else Seq(otherSubqueryJoin),
      where = Nil,
      groupBy0 = None,
    )
  }

  def aggregate[E, V](f: SelectProxy[Q] => E)
                     (implicit qr: Queryable[E, V]): Expr[V] = {
    ???
//    SimpleSelect(
//      expr = f(new SelectProxy(expr)),
//      exprPrefix = None,
//      from = Seq(this),
//      joins = Nil,
//      where = Nil,
//      groupBy0 = None
//    ).aggregate(f)
  }

  def groupBy[K, V](groupKey: Q => K)
                   (groupAggregate: SelectProxy[Q] => V)
                   (implicit qrk: Queryable[K, _], qrv: Queryable[V, _]): Select[(K, V)] = {

    val groupKeyValue = groupKey(expr)
    val Seq((_, groupKeyExpr)) = qrk.walk(groupKeyValue)
    val newExpr = (groupKeyValue, groupAggregate(new SelectProxy[Q](this.expr)))
    val groupByOpt = Some(GroupBy(groupKeyExpr, Nil))
    SimpleSelect(
      expr = newExpr,
      exprPrefix = None,
      from = Seq(new SubqueryRef[Q](this, qr)),
      joins = Nil,
      where = Nil,
      groupBy0 = groupByOpt,
    )
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
    val op2 = CompoundSelect.Op(op, other)
    if (simple(orderBy, limit, offset)) copy(compoundOps = compoundOps ++ Seq(op2))
    else CompoundSelect(SimpleSelect.from(this), Seq(op2), None, None, None)
  }

  def drop(n: Int) = copy(offset = Some(offset.getOrElse(0) + n), limit = limit.map(_ - n))
  def take(n: Int) = copy(limit = Some(limit.fold(n)(math.min(_, n))))

  override def toSqlExpr0(implicit ctx: Context): SqlStr = {
    (usql"(" + Select.SelectQueryable(qr).toSqlQuery(this, ctx) + usql")").asCompleteQuery
  }
}

object CompoundSelect {
  case class Op(op: String, rhs: Joinable[_])
}