package usql.query

import Select._
import usql.renderer.SqlStr.SqlStringSyntax
import usql.{OptionPickler, Queryable, Table}
import usql.renderer.{Context, SelectToSql, SqlStr}

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
 *
 */
case class SimpleSelect[Q](expr: Q,
                           exprPrefix: Option[String],
                           from: Seq[From],
                           joins: Seq[Join],
                           where: Seq[Expr[_]],
                           groupBy0: Option[GroupBy])
                          (implicit val qr: Queryable[Q, _]) extends Select[Q]{
  override def select = this

  def distinct: Select[Q] = this.copy(exprPrefix = Some("DISTINCT"))

  def queryExpr[V](f: Q => Context => SqlStr)
                  (implicit qr2: Queryable[Expr[V], V]): Expr[V] = {
    Expr[V] { implicit outerCtx: Context =>
      this.copy[Expr[V]](expr = Expr[V] { implicit ctx: Context =>
        val newCtx = new Context(
          outerCtx.fromNaming ++ ctx.fromNaming,
          ctx.exprNaming,
          ctx.tableNameMapper,
          ctx.columnNameMapper
        )

        f(expr)(newCtx)
      }).toSqlExpr.asCompleteQuery
    }
  }

  def map[V](f: Q => V)(implicit qr: Queryable[V, _]): Select[V] = {
    copy(expr = f(expr))
  }

  def flatMap[V](f: Q => Select[V])(implicit qr: Queryable[V, _]): Select[V] = {
//    val other = f(expr)
    ???
  }

  def filter(f: Q => Expr[Boolean]): Select[Q] = {
    if (groupBy0.isEmpty) copy(where = where ++ Seq(f(expr)))
    else copy(groupBy0 = groupBy0.map(g => g.copy(having = g.having ++ Seq(f(expr)))))
  }

  def join[V](other: Joinable[V])
             (implicit qr: Queryable[V, _]): Select[(Q, V)] = join0(other, None)

  def joinOn[V](other: Joinable[V])
               (on: (Q, V) => Expr[Boolean])
               (implicit qr: Queryable[V, _]): Select[(Q, V)] = join0(other, Some(on))

  def join0[V](other: Joinable[V],
               on: Option[(Q, V) => Expr[Boolean]])
              (implicit joinQr: Queryable[V, _]): Select[(Q, V)] = {

    val thisTrivial = groupBy0.isEmpty
    val otherTrivial = other.isInstanceOf[Table.Base]

    val otherSelect = other.select

    lazy val otherTableJoin = Join(None, Seq(JoinFrom(otherSelect.asInstanceOf[SimpleSelect[_]].from.head, on.map(_(expr, otherSelect.expr)))))
    lazy val otherSubqueryJoin = Join(None, Seq(JoinFrom(new SubqueryRef(otherSelect, joinQr), on.map(_(expr, otherSelect.expr)))))
    SimpleSelect(
      expr = (expr, otherSelect.expr),
      exprPrefix = if (thisTrivial) exprPrefix else None,
      from = if (thisTrivial) from else Seq(this.subquery),
      joins =
        (if (thisTrivial) joins else Nil) ++
        (if (otherTrivial) Seq(otherTableJoin) else Seq(otherSubqueryJoin)),
      where = if (thisTrivial) where else Nil,
      groupBy0 = if (thisTrivial) groupBy0 else None,
    )
  }

  def aggregate[E, V](f: SelectProxy[Q] => E)
                     (implicit qr: Queryable[E, V]): Expr[V] = {

    Expr[V] { implicit ctx: Context =>
      this.copy(expr = f(new SelectProxy[Q](expr))).toSqlExpr
    }
  }

  def groupBy[K, V](groupKey: Q => K)
                   (groupAggregate: SelectProxy[Q] => V)
                   (implicit qrk: Queryable[K, _], qrv: Queryable[V, _]): Select[(K, V)] = {
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
    CompoundSelect(this, None, Some(OrderBy(f(expr), None, None)), None, None)
  }

  def asc = throw new Exception(".asc must follow .sortBy")
  def desc = throw new Exception(".desc must follow .sortBy")
  def nullsFirst = throw new Exception(".nullsFirst must follow .sortBy")
  def nullsLast = throw new Exception(".nullsLast must follow .sortBy")

  def compound0(op: String, other: Select[Q]) = {
    CompoundSelect(this, Some(CompoundSelect.Op(op, other)), None, None, None)
  }

  def drop(n: Int) = CompoundSelect(this, None, None, None, Some(n))
  def take(n: Int) = CompoundSelect(this, None, None, Some(n), Some(n))

  override def toSqlExpr0(implicit ctx: Context): SqlStr = {
    (usql"(" + Select.SelectQueryable(qr).toSqlQuery(this, ctx) + usql")").asCompleteQuery
  }
}
