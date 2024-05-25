package scalasql.query

import scalasql.core.SqlStr.{Renderable, SqlStringSyntax}
import scalasql.core.{Context, DialectTypeMappers, LiveExprs, Queryable, Expr, SqlStr}

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
)(implicit val qr: Queryable.Row[Q, R], val dialect: DialectTypeMappers)
    extends Select.Proxy[Q, R] {

  protected def copy[Q, R](
      lhs: SimpleSelect[Q, R] = this.lhs,
      compoundOps: Seq[CompoundSelect.Op[Q, R]] = this.compoundOps,
      orderBy: Seq[OrderBy] = this.orderBy,
      limit: Option[Int] = this.limit,
      offset: Option[Int] = this.offset
  )(implicit qr: Queryable.Row[Q, R]) = newCompoundSelect(lhs, compoundOps, orderBy, limit, offset)
  override protected def expr = Joinable.toFromExpr(lhs)._2

  protected def selectToSimpleSelect() = this.subquery

  override def map[Q2, R2](f: Q => Q2)(implicit qr2: Queryable.Row[Q2, R2]): Select[Q2, R2] = {
    (lhs, compoundOps) match {
      case (s: SimpleSelect[Q, R], Nil) =>
        val mapped = s.map(f)
        copy[Q2, R2](mapped, Nil, orderBy, limit, offset)

      case _ => selectToSimpleSelect().map(f)
    }
  }

  override def filter(f: Q => Expr[Boolean]): Select[Q, R] = {
    (lhs, compoundOps) match {
      case (s: SimpleSelect[Q, R], Nil) =>
        copy(Select.toSimpleFrom(s.filter(f)), compoundOps, orderBy, limit, offset)
      case _ => selectToSimpleSelect().filter(f)
    }
  }

  override def sortBy(f: Q => Expr[?]) = {
    val newOrder = Seq(OrderBy(f(expr), None, None))

    if (limit.isEmpty && offset.isEmpty) copy(orderBy = newOrder ++ orderBy)
    else newCompoundSelect(selectToSimpleSelect(), compoundOps, newOrder, None, None)
  }

  override def asc: Select[Q, R] =
    copy(orderBy = orderBy.take(1).map(_.copy(ascDesc = Some(AscDesc.Asc))) ++ orderBy.drop(1))

  override def desc: Select[Q, R] =
    copy(orderBy = orderBy.take(1).map(_.copy(ascDesc = Some(AscDesc.Desc))) ++ orderBy.drop(1))

  override def nullsFirst: Select[Q, R] =
    copy(orderBy = orderBy.take(1).map(_.copy(nulls = Some(Nulls.First))) ++ orderBy.drop(1))

  override def nullsLast: Select[Q, R] =
    copy(orderBy = orderBy.take(1).map(_.copy(nulls = Some(Nulls.Last))) ++ orderBy.drop(1))

  override def compound0(op: String, other: Select[Q, R]) = {
    val op2 = CompoundSelect.Op(op, Select.toSimpleFrom(other))
    if (orderBy.isEmpty && limit.isEmpty && offset.isEmpty)
      copy(compoundOps = compoundOps ++ Seq(op2))
    else newCompoundSelect(selectToSimpleSelect(), Seq(op2), Nil, None, None)
  }

  override def drop(n: Int): Select[Q, R] =
    copy(offset = Some(offset.getOrElse(0) + n), limit = limit.map(_ - n))
  override def take(n: Int): Select[Q, R] = copy(limit = Some(limit.fold(n)(math.min(_, n))))

  override protected def selectRenderer(prevContext: Context): SubqueryRef.Wrapped.Renderer =
    new CompoundSelect.Renderer(this, prevContext)

  override protected def selectExprAliases(prevContext: Context) = {
    SubqueryRef.Wrapped.exprAliases(lhs, prevContext)
  }

}

object CompoundSelect {
  case class Op[Q, R](op: String, rhs: SimpleSelect[Q, R])

  class Renderer[Q, R](query: CompoundSelect[Q, R], prevContext: Context)
      extends SubqueryRef.Wrapped.Renderer {
    import query.dialect._
    lazy val renderer = SimpleSelect.getRenderer(query.lhs, prevContext)

    lazy val lhsExprAliases = SubqueryRef.Wrapped.exprAliases(query.lhs, prevContext)
    lazy val context = renderer.context
      .withExprNaming(renderer.context.exprNaming ++ lhsExprAliases)

    lazy val sortOpt = SqlStr.flatten(orderToSqlStr(context))

    lazy val limitOpt = SqlStr.flatten(SqlStr.opt(query.limit) { limit =>
      sql" LIMIT $limit"
    })
    lazy val offsetOpt = SqlStr.flatten(SqlStr.opt(query.offset) { offset =>
      sql" OFFSET $offset"
    })

    lazy val newReferencedExpressions = Seq(limitOpt, offsetOpt, sortOpt).flatMap(_.referencedExprs)

    // For compound expressions that eliminate duplicate columns (i.e. UNION/INTERSECT/EXCEPT,
    // everything except UNION ALL) we cannot eliminate dead columns as it affects whether
    // columns are duplicates or not, and thus what final set of rows is returned
    lazy val preserveAll = query.compoundOps.exists(_.op != "UNION ALL")

    def render(liveExprs: LiveExprs) = {
      val innerLiveExprs =
        if (preserveAll) LiveExprs.none
        else liveExprs.map(_ ++ newReferencedExpressions)

      val lhsStr = renderer.render(innerLiveExprs)
      val lhsExprIndices = lhsExprAliases.iterator.map(_._1).zipWithIndex.toMap
      val innerLiveExprIndices = innerLiveExprs.values.map(_.flatMap(lhsExprIndices.get))
      val compound = SqlStr.optSeq(query.compoundOps) { compoundOps =>
        val compoundStrs = compoundOps.map { op =>
          val rhsToSqlQuery = SimpleSelect.getRenderer(op.rhs, prevContext)
          lazy val rhsExprAliases = SubqueryRef.Wrapped.exprAliases(op.rhs, prevContext)
          // We match up the RHS SimpleSelect's exprAliases with the LHS SimpleSelect's
          // exprAliases, because the expressions in the CompoundSelect's lhsMap correspond
          // to those belonging to the LHS SimpleSelect, but we need the corresponding
          // expressions belonging to the RHS SimpleSelect `liveExprs` analysis to work
          val rhsInnerLiveExprs = rhsExprAliases.iterator.zipWithIndex.collect {
            case ((k, v), i) if innerLiveExprIndices.fold(true)(_(i)) => k
          }.toSet

          sql" ${SqlStr.raw(op.op)} ${rhsToSqlQuery.render(new LiveExprs(Some(rhsInnerLiveExprs)))}"
        }

        SqlStr.join(compoundStrs)
      }

      lhsStr + compound + sortOpt + limitOpt + offsetOpt
    }
    def orderToSqlStr(newCtx: Context) =
      CompoundSelect.orderToSqlStr(query.orderBy, newCtx, gap = true)
  }

  def orderToSqlStr(orderBys: Seq[OrderBy], newCtx: Context, gap: Boolean = false) = {

    SqlStr.optSeq(orderBys) { orderBys =>
      val orderStr = SqlStr.join(
        orderBys.map { orderBy =>
          val ascDesc = orderBy.ascDesc match {
            case None => SqlStr.empty
            case Some(AscDesc.Asc) => sql" ASC"
            case Some(AscDesc.Desc) => sql" DESC"
          }

          val nulls = SqlStr.opt(orderBy.nulls) {
            case Nulls.First => sql" NULLS FIRST"
            case Nulls.Last => sql" NULLS LAST"
          }
          Renderable.renderSql(orderBy.expr)(newCtx) + ascDesc + nulls
        },
        SqlStr.commaSep
      )

      val prefix = if (gap) sql" ORDER BY " else sql"ORDER BY "
      prefix + orderStr
    }

  }
}
