package scalasql.query

import scalasql.renderer.SqlStr.SqlStringSyntax
import scalasql.renderer.{Context, SelectToSql, SqlStr}
import scalasql.utils.OptionPickler
import scalasql.{MappedType, Queryable, Table}

class CompoundSelect[Q, R](
    val lhs: SimpleSelect[Q, R],
    val compoundOps: Seq[CompoundSelect.Op[Q, R]],
    val orderBy: Seq[OrderBy],
    val limit: Option[Int],
    val offset: Option[Int]
)(implicit val qr: Queryable[Q, R])
    extends Select[Q, R] {

  protected def copy[Q, R](
      lhs: SimpleSelect[Q, R] = this.lhs,
      compoundOps: Seq[CompoundSelect.Op[Q, R]] = this.compoundOps,
      orderBy: Seq[OrderBy] = this.orderBy,
      limit: Option[Int] = this.limit,
      offset: Option[Int] = this.offset
  )(implicit qr: Queryable[Q, R]) = newCompoundSelect(lhs, compoundOps, orderBy, limit, offset)
  def expr = lhs.select.expr

  override def select = this

  def distinct: Select[Q, R] = simpleFrom(this).distinct

  def queryExpr[V: MappedType](f: Q => Context => SqlStr)(
      implicit qr: Queryable[Expr[V], V]
  ): Expr[V] = simpleFrom(this).queryExpr[V](f)

  def map[Q2, R2](f: Q => Q2)(implicit qr2: Queryable[Q2, R2]): Select[Q2, R2] = {
    (lhs, compoundOps) match {
      case (s: Select[Q, R], Nil) => copy(simpleFrom(s.map(f)), Nil, orderBy, limit, offset)

      case _ => simpleFrom(this).map(f)
    }
  }

  def flatMap[Q2, R2](f: Q => Select[Q2, R2])(implicit qr: Queryable[Q2, R2]): Select[Q2, R2] = {
    simpleFrom(this).flatMap(f)
  }

  def filter(f: Q => Expr[Boolean]): Select[Q, R] = {
    (lhs, compoundOps) match {
      case (s: SimpleSelect[Q, R], Nil) =>
        copy(simpleFrom(s.filter(f)), compoundOps, orderBy, limit, offset)
      case _ => simpleFrom(this).filter(f)
    }
  }

  def join0[Q2, R2](other: Joinable[Q2, R2], on: Option[(Q, Q2) => Expr[Boolean]])(
      implicit joinQr: Queryable[Q2, R2]
  ): Select[(Q, Q2), (R, R2)] = { simpleFrom(this).join0(other, on) }

  def aggregate[E, V](f: SelectProxy[Q] => E)(implicit qr: Queryable[E, V]): Aggregate[E, V] = {
    simpleFrom(this).aggregate(f)
  }

  def groupBy[K, V, R1, R2](groupKey: Q => K)(
      groupAggregate: SelectProxy[Q] => V
  )(implicit qrk: Queryable[K, R1], qrv: Queryable[V, R2]): Select[(K, V), (R1, R2)] = {
    simpleFrom(this).groupBy(groupKey)(groupAggregate)
  }

  def sortBy(f: Q => Expr[_]) = {
    val newOrder = Seq(OrderBy(f(expr), None, None))

    if (simple(limit, offset)) copy(orderBy = newOrder ++ orderBy)
    else newCompoundSelect(simpleFrom(this), compoundOps, newOrder, None, None)
  }

  def asc =
    copy(orderBy = orderBy.take(1).map(_.copy(ascDesc = Some(AscDesc.Asc))) ++ orderBy.drop(1))
  def desc =
    copy(orderBy = orderBy.take(1).map(_.copy(ascDesc = Some(AscDesc.Desc))) ++ orderBy.drop(1))
  def nullsFirst =
    copy(orderBy = orderBy.take(1).map(_.copy(nulls = Some(Nulls.First))) ++ orderBy.drop(1))
  def nullsLast =
    copy(orderBy = orderBy.take(1).map(_.copy(nulls = Some(Nulls.Last))) ++ orderBy.drop(1))

  def compound0(op: String, other: Select[Q, R]) = {
    val op2 = CompoundSelect.Op(op, simpleFrom(other))
    if (simple(orderBy, limit, offset)) copy(compoundOps = compoundOps ++ Seq(op2))
    else newCompoundSelect(simpleFrom(this), Seq(op2), Nil, None, None)
  }

  def drop(n: Int) = copy(offset = Some(offset.getOrElse(0) + n), limit = limit.map(_ - n))
  def take(n: Int) = copy(limit = Some(limit.fold(n)(math.min(_, n))))

  def valueReader = OptionPickler.SeqLikeReader(qr.valueReader(expr), implicitly)

  def toSqlQuery0(prevContext: Context) = new CompoundSelect.RenderInfo(this, prevContext)
}

object CompoundSelect {
  case class Op[Q, R](op: String, rhs: SimpleSelect[Q, R])

  class RenderInfo[Q, R](query: CompoundSelect[Q, R], prevContext: Context) extends Select.RenderInfo {

    val lhsToSqlQuery = query.lhs.toSqlQuery0(prevContext)


    val lhsMap = lhsToSqlQuery.lhsMap

    val context = lhsToSqlQuery.context

    val mappedTypes = lhsToSqlQuery.mappedTypes


    val newCtx = context.copy(exprNaming = context.exprNaming ++ lhsMap)

    val sortOpt = orderToSqlStr(newCtx)

    val (limitOpt, offsetOpt) = limitOffsetToSqlStr

    def render(liveExprs: Option[Set[Expr.Identity]]) = {
      val newReferencedExpressions = SqlStr.flatten(limitOpt + offsetOpt + sortOpt)
        .referencedExprs
      val preserveAll = query.compoundOps.exists(_.op != "UNION ALL")

      val innerLiveExprs = if (preserveAll) None else liveExprs.map(_ ++ newReferencedExpressions)
      val lhsStr =
        if (query.lhs.isInstanceOf[CompoundSelect[_, _]])
          sql"(${lhsToSqlQuery.render(innerLiveExprs)})"
        else lhsToSqlQuery.render(innerLiveExprs)

      val compound = SqlStr.optSeq(query.compoundOps) { compoundOps =>
        val compoundStrs = compoundOps.map { op =>
          val rhsToSqlQuery = op.rhs.toSqlQuery0(prevContext)

          // We match up the RHS SimpleSelect's lhsMap with the LHS SimpleSelect's lhsMap,
          // because the expressions in the CompoundSelect's lhsMap correspond to those
          // belonging to the LHS SimpleSelect, but we need the corresponding expressions
          // belongong to the RHS SimpleSelect `liveExprs` analysis to work
          val rhsInnerLiveExprs = innerLiveExprs.map { l =>
            val strs = l
              .map(e => SqlStr.flatten(lhsToSqlQuery.lhsMap(e)).queryParts.mkString("?"))

            rhsToSqlQuery.lhsMap.collect {
              case (k, v) if strs.contains(SqlStr.flatten(v).queryParts.mkString("?")) => k
            }.toSet
          }
          sql" ${SqlStr.raw(op.op)} ${rhsToSqlQuery.render(rhsInnerLiveExprs)}"
        }

        SqlStr.join(compoundStrs)
      }

      lhsStr + compound + sortOpt + limitOpt + offsetOpt
    }


    def limitOffsetToSqlStr = {
      val limitOpt = SqlStr.opt(query.limit) { limit => sql" LIMIT " + SqlStr.raw(limit.toString) }

      val offsetOpt = SqlStr.opt(query.offset) { offset =>
        sql" OFFSET " + SqlStr.raw(offset.toString)
      }
      (limitOpt, offsetOpt)
    }

    def orderToSqlStr(newCtx: Context) = {

      SqlStr.optSeq(query.orderBy) { orderBys =>
        val orderStr = SqlStr.join(
          orderBys.map { orderBy =>
            val ascDesc = orderBy.ascDesc match {
              case None => sql""
              case Some(AscDesc.Asc) => sql" ASC"
              case Some(AscDesc.Desc) => sql" DESC"
            }

            val nulls = SqlStr.opt(orderBy.nulls) {
              case Nulls.First => sql" NULLS FIRST"
              case Nulls.Last => sql" NULLS LAST"
            }
            orderBy.expr.toSqlQuery(newCtx)._1 + ascDesc + nulls
          },
          sql", "
        )

        sql" ORDER BY " + orderStr
      }

    }
  }
}
