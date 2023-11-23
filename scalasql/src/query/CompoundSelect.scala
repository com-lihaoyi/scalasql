package scalasql.query

import scalasql.renderer.SqlStr.{Renderable, SqlStringSyntax}
import scalasql.renderer.{Context, JoinsToSql, SqlStr}
import scalasql.utils.OptionPickler
import scalasql.{TypeMapper, Queryable, Table}

/**
 * A SQL `SELECT` query, with
 * `ORDER BY`, `LIMIT`, `OFFSET`, or `UNION` clauses
 */
class CompoundSelect[Q, R](
    val lhs: SimpleSelect[Q, R],
    val compoundOps: Seq[CompoundSelect.Op[Q, R]],
    val orderBy: Seq[OrderBy],
    val limit: Option[Int],
    val offset: Option[Int]
)(implicit val qr: Queryable.Row[Q, R])
    extends Select[Q, R] {

  protected def copy[Q, R](
      lhs: SimpleSelect[Q, R] = this.lhs,
      compoundOps: Seq[CompoundSelect.Op[Q, R]] = this.compoundOps,
      orderBy: Seq[OrderBy] = this.orderBy,
      limit: Option[Int] = this.limit,
      offset: Option[Int] = this.offset
  )(implicit qr: Queryable.Row[Q, R]) = newCompoundSelect(lhs, compoundOps, orderBy, limit, offset)
  protected def expr = WithExpr.get(Joinable.joinableSelect(lhs))

  protected override def joinableSelect = this

  def distinct: Select[Q, R] = selectSimpleFrom().distinct
  protected def selectSimpleFrom() = this.subquery
  def queryExpr[V: TypeMapper](f: Q => Context => SqlStr)(
      implicit qr: Queryable.Row[Expr[V], V]
  ): Expr[V] = selectSimpleFrom().queryExpr[V](f)

  def map[Q2, R2](f: Q => Q2)(implicit qr2: Queryable.Row[Q2, R2]): Select[Q2, R2] = {
    (lhs, compoundOps) match {
      case (s: SimpleSelect[Q, R], Nil) =>
        val mapped = s.map(f)
        copy[Q2, R2](mapped, Nil, orderBy, limit, offset)

      case _ => selectSimpleFrom().map(f)
    }
  }

  def flatMap[Q2, R2](f: Q => FlatJoin.Rhs[Q2, R2])(
      implicit qr: Queryable.Row[Q2, R2]
  ): Select[Q2, R2] = { selectSimpleFrom().flatMap(f) }

  def filter(f: Q => Expr[Boolean]): Select[Q, R] = {
    (lhs, compoundOps) match {
      case (s: SimpleSelect[Q, R], Nil) =>
        copy(Select.selectSimpleFrom(s.filter(f)), compoundOps, orderBy, limit, offset)
      case _ => selectSimpleFrom().filter(f)
    }
  }

  def join0[Q2, R2](
      prefix: String,
      other: Joinable[Q2, R2],
      on: Option[(Q, Q2) => Expr[Boolean]]
  )(
      implicit joinQr: Queryable.Row[Q2, R2]
  ): Select[(Q, Q2), (R, R2)] = { selectSimpleFrom().join0(prefix, other, on) }

  def leftJoin[Q2, R2](other: Joinable[Q2, R2])(on: (Q, Q2) => Expr[Boolean])(
      implicit joinQr: Queryable.Row[Q2, R2]
  ): Select[(Q, JoinNullable[Q2]), (R, Option[R2])] = { selectSimpleFrom().leftJoin(other)(on) }

  def rightJoin[Q2, R2](other: Joinable[Q2, R2])(on: (Q, Q2) => Expr[Boolean])(
      implicit joinQr: Queryable.Row[Q2, R2]
  ): Select[(JoinNullable[Q], Q2), (Option[R], R2)] = { selectSimpleFrom().rightJoin(other)(on) }

  def outerJoin[Q2, R2](other: Joinable[Q2, R2])(on: (Q, Q2) => Expr[Boolean])(
      implicit joinQr: Queryable.Row[Q2, R2]
  ): Select[(JoinNullable[Q], JoinNullable[Q2]), (Option[R], Option[R2])] = {
    selectSimpleFrom().outerJoin(other)(on)
  }

  def aggregate[E, V](f: SelectProxy[Q] => E)(implicit qr: Queryable.Row[E, V]): Aggregate[E, V] = {
    selectSimpleFrom().aggregate(f)
  }

  def groupBy[K, V, R1, R2](groupKey: Q => K)(
      groupAggregate: SelectProxy[Q] => V
  )(implicit qrk: Queryable.Row[K, R1], qrv: Queryable.Row[V, R2]): Select[(K, V), (R1, R2)] = {
    selectSimpleFrom().groupBy(groupKey)(groupAggregate)
  }

  def sortBy(f: Q => Expr[_]) = {
    val newOrder = Seq(OrderBy(f(expr), None, None))

    if (limit.isEmpty && offset.isEmpty) copy(orderBy = newOrder ++ orderBy)
    else newCompoundSelect(selectSimpleFrom(), compoundOps, newOrder, None, None)
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
    val op2 = CompoundSelect.Op(op, Select.selectSimpleFrom(other))
    if (orderBy.isEmpty && limit.isEmpty && offset.isEmpty)
      copy(compoundOps = compoundOps ++ Seq(op2))
    else newCompoundSelect(selectSimpleFrom(), Seq(op2), Nil, None, None)
  }

  def drop(n: Int) = copy(offset = Some(offset.getOrElse(0) + n), limit = limit.map(_ - n))
  def take(n: Int) = copy(limit = Some(limit.fold(n)(math.min(_, n))))

  protected def queryValueReader = OptionPickler.SeqLikeReader2(qr.valueReader(expr), implicitly)

  protected def selectRenderer(prevContext: Context) =
    new CompoundSelect.Renderer(this, prevContext)
  protected override def queryTypeMappers() = qr.toTypeMappers(expr)

  protected def selectLhsMap(prevContext: Context): Map[Expr.Identity, SqlStr] = {
    Select.selectLhsMap(lhs, prevContext)
  }
}

object CompoundSelect {
  case class Op[Q, R](op: String, rhs: SimpleSelect[Q, R])

  class Renderer[Q, R](query: CompoundSelect[Q, R], prevContext: Context) extends Select.Renderer {

    lazy val lhsToSqlQuery = SimpleSelect.getRenderer(query.lhs, prevContext)

    lazy val lhsLhsMap = Select.selectLhsMap(query.lhs, prevContext)
    lazy val newCtx = lhsToSqlQuery.context
      .withExprNaming(lhsToSqlQuery.context.exprNaming ++ lhsLhsMap)

    lazy val sortOpt = SqlStr.flatten(orderToSqlStr(newCtx))

    lazy val limitOpt = SqlStr.flatten(SqlStr.opt(query.limit) { limit =>
      sql" LIMIT $limit"
    })
    lazy val offsetOpt = SqlStr.flatten(SqlStr.opt(query.offset) { offset =>
      sql" OFFSET $offset"
    })

    lazy val newReferencedExpressions = Seq(limitOpt, offsetOpt, sortOpt).flatMap(_.referencedExprs)

    lazy val preserveAll = query.compoundOps.exists(_.op != "UNION ALL")

    def render(liveExprs: Option[Set[Expr.Identity]]) = {

      val innerLiveExprs = if (preserveAll) None else liveExprs.map(_ ++ newReferencedExpressions)
      val lhsStr = lhsToSqlQuery.render(innerLiveExprs)

      val compound = SqlStr.optSeq(query.compoundOps) { compoundOps =>
        val compoundStrs = compoundOps.map { op =>
          val rhsToSqlQuery = SimpleSelect.getRenderer(op.rhs, prevContext)
          lazy val rhsLhsMap = Select.selectLhsMap(op.rhs, prevContext)
          // We match up the RHS SimpleSelect's lhsMap with the LHS SimpleSelect's lhsMap,
          // because the expressions in the CompoundSelect's lhsMap correspond to those
          // belonging to the LHS SimpleSelect, but we need the corresponding expressions
          // belongong to the RHS SimpleSelect `liveExprs` analysis to work
          val rhsInnerLiveExprs = innerLiveExprs.map { l =>
            val strs = l.map(e => SqlStr.flatten(lhsLhsMap(e)).queryParts.mkString("?"))

            rhsLhsMap.collect {
              case (k, v) if strs.contains(SqlStr.flatten(v).queryParts.mkString("?")) => k
            }.toSet
          }
          sql" ${SqlStr.raw(op.op)} ${rhsToSqlQuery.render(rhsInnerLiveExprs)}"
        }

        SqlStr.join(compoundStrs)
      }

      lhsStr + compound + sortOpt + limitOpt + offsetOpt
    }
    def orderToSqlStr(newCtx: Context) = CompoundSelect.orderToSqlStr(query.orderBy, newCtx, gap = true)
  }

  def orderToSqlStr(orderBys: Seq[OrderBy], newCtx: Context, gap: Boolean = false) = {

    SqlStr.optSeq(orderBys) { orderBys =>
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
          Renderable.renderToSql(orderBy.expr)(newCtx) + ascDesc + nulls
        },
        sql", "
      )

      val prefix = if (gap) sql" ORDER BY " else sql"ORDER BY "
      prefix + orderStr
    }

  }
}
