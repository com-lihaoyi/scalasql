package scalasql.query

import scalasql.renderer.SelectToSql.joinsToSqlStr
import scalasql.renderer.SqlStr.SqlStringSyntax
import scalasql.{MappedType, Queryable}
import scalasql.renderer.{Context, ExprsToSql, SqlStr}
import scalasql.utils.{FlatJson, OptionPickler}

/**
 * Models the various components of a SQL query:
 *
 * {{{
 *  SELECT DISTINCT column, AGG_FUNC(column_or_expression), …
 *  FROM mytable
 *  JOIN another_table ON mytable.column = another_table.column
 *  WHERE constraint_expression
 *  GROUP BY column HAVING constraint_expression
 *  ORDER BY column ASC/DESC
 *  LIMIT count OFFSET COUNT;
 * }}}
 *
 * Good syntax reference:
 *
 * https://www.cockroachlabs.com/docs/stable/selection-queries#set-operations
 * https://www.postgresql.org/docs/current/sql-select.html
 */
case class SimpleSelect[Q, R](
    expr: Q,
    exprPrefix: Option[String],
    from: Seq[From],
    joins: Seq[Join],
    where: Seq[Expr[_]],
    groupBy0: Option[GroupBy]
)(implicit val qr: Queryable[Q, R])
    extends Select[Q, R] {
  override def select = this

  def distinct: Select[Q, R] = this.copy(exprPrefix = Some("DISTINCT"))

  def queryExpr[V: MappedType](
      f: Q => Context => SqlStr
  )(implicit qr2: Queryable[Expr[V], V]): Expr[V] = {
    Expr[V] { implicit outerCtx: Context =>
      this.copy(expr = Expr[V] { implicit ctx: Context =>
        val newCtx = ctx.copy(fromNaming = outerCtx.fromNaming ++ ctx.fromNaming)

        f(expr)(newCtx)
      }).toSqlQuery._1.withCompleteQuery(true)
    }
  }

  def map[Q2, R2](f: Q => Q2)(implicit qr: Queryable[Q2, R2]): Select[Q2, R2] = copy(expr = f(expr))

  def flatMap[Q2, R2](f: Q => Select[Q2, R2])(implicit qr2: Queryable[Q2, R2]): Select[Q2, R2] = {
    val other = f(expr)
    val simple = SimpleSelect.from(other)
    simple.copy(from = this.from ++ simple.from)
  }

  def filter(f: Q => Expr[Boolean]): Select[Q, R] = {
    if (groupBy0.isEmpty) copy(where = where ++ Seq(f(expr)))
    else copy(groupBy0 = groupBy0.map(g => g.copy(having = g.having ++ Seq(f(expr)))))
  }

  def join0[Q2, R2](other: Joinable[Q2, R2], on: Option[(Q, Q2) => Expr[Boolean]])(
      implicit joinQr: Queryable[Q2, R2]
  ): Select[(Q, Q2), (R, R2)] = {

    val thisTrivial = groupBy0.isEmpty
    val (otherJoin, otherSelect) = joinInfo(other, on)

    SimpleSelect(
      expr = (expr, otherSelect.expr),
      exprPrefix = if (thisTrivial) exprPrefix else None,
      from = if (thisTrivial) from else Seq(this.subquery),
      joins = (if (thisTrivial) joins else Nil) ++ otherJoin,
      where = if (thisTrivial) where else Nil,
      groupBy0 = if (thisTrivial) groupBy0 else None
    )
  }

  def aggregate[E, V](f: SelectProxy[Q] => E)(implicit qr: Queryable[E, V]): Aggregate[E, V] = {
    val selectProxyExpr = f(new SelectProxy[Q](expr))
    new Aggregate[E, V](
      implicit ctx => this.copy(expr = selectProxyExpr).toSqlQuery,
      selectProxyExpr
    )(qr)
  }

  def groupBy[K, V, R1, R2](groupKey: Q => K)(
      groupAggregate: SelectProxy[Q] => V
  )(implicit qrk: Queryable[K, R1], qrv: Queryable[V, R2]): Select[(K, V), (R1, R2)] = {
    val groupKeyValue = groupKey(expr)
    val Seq((_, groupKeyExpr)) = qrk.walk(groupKeyValue)
    val newExpr = (groupKeyValue, groupAggregate(new SelectProxy[Q](this.expr)))
    val groupByOpt = Some(GroupBy(groupKeyExpr, Nil))
    if (groupBy0.isEmpty) this.copy(expr = newExpr, groupBy0 = groupByOpt)
    else SimpleSelect(
      expr = newExpr,
      exprPrefix = exprPrefix,
      from = Seq(this.subquery),
      joins = Nil,
      where = Nil,
      groupBy0 = groupByOpt
    )
  }

  def sortBy(f: Q => Expr[_]) = {
    CompoundSelect(this, Nil, Some(OrderBy(f(expr), None, None)), None, None)
  }

  def asc = throw new Exception(".asc must follow .sortBy")
  def desc = throw new Exception(".desc must follow .sortBy")
  def nullsFirst = throw new Exception(".nullsFirst must follow .sortBy")
  def nullsLast = throw new Exception(".nullsLast must follow .sortBy")

  def compound0(op: String, other: Select[Q, R]) = {
    val op2 = CompoundSelect.Op(op, SimpleSelect.from(other))
    CompoundSelect(this, Seq(op2), None, None, None)
  }

  def drop(n: Int) = CompoundSelect(this, Nil, None, None, Some(n))
  def take(n: Int) = CompoundSelect(this, Nil, None, Some(n), None)

  def valueReader = OptionPickler.SeqLikeReader(qr.valueReader(expr), implicitly)

  def toSqlQuery0(prevContext: Context) = SimpleSelect.toSqlStr(this, qr, prevContext)
}

object SimpleSelect {
  def from[Q, R](s: Select[Q, R]) = s match {
    case s: SimpleSelect[Q, R] => s
    case s: CompoundSelect[Q, R] =>
      SimpleSelect(s.expr, None, Seq(s.subquery(s.qr)), Nil, Nil, None)(s.qr)
  }

  def toSqlStr[Q, R](
      query: SimpleSelect[Q, R],
      qr: Queryable[Q, R],
      prevContext: Context
  ): (Map[Expr.Identity, SqlStr], SqlStr, Context, Seq[MappedType[_]]) = {
    val computed = Context
      .compute(prevContext, query.from ++ query.joins.flatMap(_.from.map(_.from)), None)

    import computed.implicitCtx

    val exprPrefix = SqlStr.opt(query.exprPrefix) { p => SqlStr.raw(p) + sql" " }
    val (flattenedExpr, exprStr) = ExprsToSql(qr.walk(query.expr), exprPrefix, implicitCtx)

    val tables = SqlStr.join(query.from.map(computed.fromSelectables(_)._2), sql", ")

    val joins = joinsToSqlStr(query.joins, computed.fromSelectables)

    val filtersOpt = SqlStr.optSeq(query.where) { where =>
      sql" WHERE " + SqlStr.join(where.map(_.toSqlQuery._1), sql" AND ")
    }

    val groupByOpt = SqlStr.opt(query.groupBy0) { groupBy =>
      val havingOpt = SqlStr.optSeq(groupBy.having) { having =>
        sql" HAVING " + SqlStr.join(having.map(_.toSqlQuery._1), sql" AND ")
      }
      sql" GROUP BY ${groupBy.expr}${havingOpt}"
    }

    val jsonQueryMap = flattenedExpr.map { case (k, v) =>
      (
        Expr.getIdentity(v),
        SqlStr.raw(
          (prevContext.config.columnLabelPrefix +: k).map(prevContext.config.columnNameMapper)
            .mkString(prevContext.config.columnLabelDelimiter)
        )
      )
    }.toMap

    (
      jsonQueryMap,
      exprStr + sql" FROM " + tables + joins + filtersOpt + groupByOpt,
      implicitCtx,
      flattenedExpr.map(t => Expr.getMappedType(t._2))
    )
  }
}
