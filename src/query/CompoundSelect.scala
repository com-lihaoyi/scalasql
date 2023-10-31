package scalasql.query

import scalasql.renderer.SqlStr.SqlStringSyntax
import scalasql.renderer.{Context, SelectToSql, SqlStr}
import scalasql.utils.OptionPickler
import scalasql.{MappedType, Queryable, Table}

class CompoundSelect[Q, R](
    val lhs: SimpleSelect[Q, R],
    val compoundOps: Seq[CompoundSelect.Op[Q, R]],
    val orderBy: Option[OrderBy],
    val limit: Option[Int],
    val offset: Option[Int]
)(implicit val qr: Queryable[Q, R])
    extends Select[Q, R] {

  protected def copy[Q, R](
      lhs: SimpleSelect[Q, R] = this.lhs,
      compoundOps: Seq[CompoundSelect.Op[Q, R]] = this.compoundOps,
      orderBy: Option[OrderBy] = this.orderBy,
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
    val newOrder = Some(OrderBy(f(expr), None, None))

    if (simple(limit, offset)) copy(orderBy = newOrder)
    else newCompoundSelect(simpleFrom(this), compoundOps, newOrder, None, None)
  }

  def asc = copy(orderBy = Some(orderBy.get.copy(ascDesc = Some(AscDesc.Asc))))
  def desc = copy(orderBy = Some(orderBy.get.copy(ascDesc = Some(AscDesc.Desc))))
  def nullsFirst = copy(orderBy = Some(orderBy.get.copy(nulls = Some(Nulls.First))))
  def nullsLast = copy(orderBy = Some(orderBy.get.copy(nulls = Some(Nulls.Last))))

  def compound0(op: String, other: Select[Q, R]) = {
    val op2 = CompoundSelect.Op(op, simpleFrom(other))
    if (simple(orderBy, limit, offset)) copy(compoundOps = compoundOps ++ Seq(op2))
    else newCompoundSelect(simpleFrom(this), Seq(op2), None, None, None)
  }

  def drop(n: Int) = copy(offset = Some(offset.getOrElse(0) + n), limit = limit.map(_ - n))
  def take(n: Int) = copy(limit = Some(limit.fold(n)(math.min(_, n))))

  def valueReader = OptionPickler.SeqLikeReader(qr.valueReader(expr), implicitly)

  def toSqlQuery0(prevContext: Context) = new CompoundSelect.Renderer(this, prevContext).toSqlStr()
}

object CompoundSelect {
  case class Op[Q, R](op: String, rhs: SimpleSelect[Q, R])

  class Renderer[Q, R](query: CompoundSelect[Q, R], prevContext: Context) {

    def toSqlStr(): (Map[Expr.Identity, SqlStr], SqlStr, Context, Seq[MappedType[_]]) = {
      val (lhsMap, lhsStr0, context, mappedTypes) = query.lhs.toSqlQuery0(prevContext)

      val lhsStr = if (query.lhs.isInstanceOf[CompoundSelect[_, _]]) sql"($lhsStr0)" else lhsStr0
      implicit val ctx = context

      val compound = SqlStr.optSeq(query.compoundOps) { compoundOps =>
        val compoundStrs = compoundOps.map { op =>
          val (compoundMapping, compoundStr, compoundCtx, compoundMappedTypes) = op.rhs
            .toSqlQuery0(prevContext)

          sql" ${SqlStr.raw(op.op)} $compoundStr"
        }

        SqlStr.join(compoundStrs)
      }

      val newCtx = context.copy(exprNaming = context.exprNaming ++ lhsMap)

      val sortOpt = orderToToSqlStr(newCtx)

      val (limitOpt, offsetOpt) = limitOffsetToSqlStr

      val res = lhsStr + compound + sortOpt + limitOpt + offsetOpt

      (lhsMap, res, context, mappedTypes)
    }

    def limitOffsetToSqlStr = {
      val limitOpt = SqlStr.opt(query.limit) { limit => sql" LIMIT " + SqlStr.raw(limit.toString) }

      val offsetOpt = SqlStr.opt(query.offset) { offset =>
        sql" OFFSET " + SqlStr.raw(offset.toString)
      }
      (limitOpt, offsetOpt)
    }

    def orderToToSqlStr[R, Q](newCtx: Context) = {
      SqlStr.opt(query.orderBy) { orderBy =>
        val ascDesc = orderBy.ascDesc match {
          case None => sql""
          case Some(AscDesc.Asc) => sql" ASC"
          case Some(AscDesc.Desc) => sql" DESC"
        }

        val nulls = SqlStr.opt(orderBy.nulls) {
          case Nulls.First => sql" NULLS FIRST"
          case Nulls.Last => sql" NULLS LAST"
        }

        sql" ORDER BY " + orderBy.expr.toSqlQuery(newCtx)._1 + ascDesc + nulls
      }
    }
  }
}
