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

  def toSqlQuery0(prevContext: Context) = new CompoundSelect.Renderer(this, prevContext).toSqlStr()
}

object CompoundSelect {
  case class Op[Q, R](op: String, rhs: SimpleSelect[Q, R])

  class Renderer[Q, R](query: CompoundSelect[Q, R], prevContext: Context) {

    def toSqlStr(): Select.Info = new Select.Info {
      lazy val lhsToSqlQuery = query.lhs.toSqlQuery0(prevContext)


      import lhsToSqlQuery.context


      lazy val newCtx = context.copy(exprNaming = context.exprNaming ++ lhsMap)

      lazy val sortOpt = orderToToSqlStr(newCtx)

      lazy val (limitOpt, offsetOpt) = limitOffsetToSqlStr

      def res(liveExprs: Option[Set[Expr.Identity]]) = {
        lazy val lhsStr =
          if (query.lhs.isInstanceOf[CompoundSelect[_, _]]) sql"(${lhsToSqlQuery.res(liveExprs)})"
          else lhsToSqlQuery.res(liveExprs)

        lazy val compound = SqlStr.optSeq(query.compoundOps) { compoundOps =>
          val compoundStrs = compoundOps.map { op =>
            val rhsToSqlQuery = op.rhs.toSqlQuery0(prevContext)

            sql" ${SqlStr.raw(op.op)} ${rhsToSqlQuery.res(liveExprs)}"
          }

          SqlStr.join(compoundStrs)
        }

        lhsStr + compound + sortOpt + limitOpt + offsetOpt
      }

      lazy val lhsMap = lhsToSqlQuery.lhsMap

      lazy val context = lhsToSqlQuery.context

      lazy val mappedTypes = lhsToSqlQuery.mappedTypes
    }

    def limitOffsetToSqlStr = {
      val limitOpt = SqlStr.opt(query.limit) { limit => sql" LIMIT " + SqlStr.raw(limit.toString) }

      val offsetOpt = SqlStr.opt(query.offset) { offset =>
        sql" OFFSET " + SqlStr.raw(offset.toString)
      }
      (limitOpt, offsetOpt)
    }

    def orderToToSqlStr[R, Q](newCtx: Context) = {
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
