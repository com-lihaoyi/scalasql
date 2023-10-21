package usql.query

import usql.Queryable
import usql.renderer.{Context, SqlStr}

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
case class SimpleSelect[Q](
    expr: Q,
    exprPrefix: Option[String],
    from: Seq[From],
    joins: Seq[Join],
    where: Seq[Expr[_]],
    groupBy0: Option[GroupBy]
)(implicit val qr: Queryable[Q, _]) extends Select[Q] {
  override def select = this

  def distinct: Select[Q] = this.copy(exprPrefix = Some("DISTINCT"))

  def queryExpr[V](f: Q => Context => SqlStr)(implicit qr2: Queryable[Expr[V], V]): Expr[V] = {
    Expr[V] { implicit outerCtx: Context =>
      this.copy[Expr[V]](expr = Expr[V] { implicit ctx: Context =>
        val newCtx = ctx.withAddedFromNaming(outerCtx.fromNaming)

        f(expr)(newCtx)
      }).toSqlStr.withCompleteQuery(true)
    }
  }

  def map[V](f: Q => V)(implicit qr: Queryable[V, _]): Select[V] = copy(expr = f(expr))

  def flatMap[V](f: Q => Select[V])(implicit qr2: Queryable[V, _]): Select[V] = {
    val other = f(expr)
    val simple = SimpleSelect.from(other)
    simple.copy(from = this.from ++ simple.from)
  }

  def filter(f: Q => Expr[Boolean]): Select[Q] = {
    if (groupBy0.isEmpty) copy(where = where ++ Seq(f(expr)))
    else copy(groupBy0 = groupBy0.map(g => g.copy(having = g.having ++ Seq(f(expr)))))
  }

  def join0[V](other: Joinable[V], on: Option[(Q, V) => Expr[Boolean]])(implicit
      joinQr: Queryable[V, _]
  ): Select[(Q, V)] = {

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

  def aggregate[E, V](f: SelectProxy[Q] => E)(implicit qr: Queryable[E, V]): Expr[V] = {

    Expr[V] { implicit ctx: Context =>
      this.copy(expr = f(new SelectProxy[Q](expr))).toSqlStr
    }
  }

  def groupBy[K, V](groupKey: Q => K)(groupAggregate: SelectProxy[Q] => V)(implicit
      qrk: Queryable[K, _],
      qrv: Queryable[V, _]
  ): Select[(K, V)] = {
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

  def compound0(op: String, other: Select[Q]) = {
    val op2 = CompoundSelect.Op(op, SimpleSelect.from(other))
    CompoundSelect(this, Seq(op2), None, None, None)
  }

  def drop(n: Int) = CompoundSelect(this, Nil, None, None, Some(n))
  def take(n: Int) = CompoundSelect(this, Nil, None, Some(n), None)

  override def toSqlExpr0(implicit ctx: Context): SqlStr = {
    Select.SelectQueryable(qr).toSqlQuery(this, ctx).withCompleteQuery(true)
  }
}

object SimpleSelect {
  def from[Q](s: Select[Q]) = s match {
    case s: SimpleSelect[Q] => s
    case s: CompoundSelect[Q] =>
      SimpleSelect(s.expr, None, Seq(s.subquery(s.qr)), Nil, Nil, None)(s.qr)
  }
}
