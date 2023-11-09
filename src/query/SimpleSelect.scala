package scalasql.query

import scalasql.renderer.JoinsToSql.joinsToSqlStr
import scalasql.renderer.SqlStr.SqlStringSyntax
import scalasql.{Config, MappedType, Queryable}
import scalasql.renderer.{Context, ExprsToSql, SqlStr}
import scalasql.utils.{FlatJson, OptionPickler}

/**
 * A `SELECT` query, with `FROM`/`JOIN`/`WHERE`/`GROUP BY`
 * clauses, but without `ORDER BY`/`LIMIT`/`TAKE`/`UNION` clauses
 */
class SimpleSelect[Q, R](
    val expr: Q,
    val exprPrefix: Option[String],
    val from: Seq[From],
    val joins: Seq[Join],
    val where: Seq[Expr[_]],
    val groupBy0: Option[GroupBy]
)(implicit val qr: Queryable.Row[Q, R])
    extends Select[Q, R] {
  override def select = this

  def copy[Q, R](
      expr: Q = this.expr,
      exprPrefix: Option[String] = this.exprPrefix,
      from: Seq[From] = this.from,
      joins: Seq[Join] = this.joins,
      where: Seq[Expr[_]] = this.where,
      groupBy0: Option[GroupBy] = this.groupBy0
  )(implicit qr: Queryable.Row[Q, R]) =
    newSimpleSelect(expr, exprPrefix, from, joins, where, groupBy0)
  def distinct: Select[Q, R] = this.copy(exprPrefix = Some("DISTINCT"))

  def queryExpr[V: MappedType](
      f: Q => Context => SqlStr
  )(implicit qr2: Queryable.Row[Expr[V], V]): Expr[V] = {
    Expr[V] { implicit outerCtx: Context =>
      this
        .copy(expr = Expr[V] { implicit ctx: Context =>
          val newCtx = ctx.withFromNaming(outerCtx.fromNaming ++ ctx.fromNaming)

          f(expr)(newCtx)
        })
        .toSqlQuery
        ._1
        .withCompleteQuery(true)
    }
  }

  def map[Q2, R2](f: Q => Q2)(implicit qr: Queryable.Row[Q2, R2]): SimpleSelect[Q2, R2] =
    copy(expr = f(expr))

  def flatMap[Q2, R2](
      f: Q => FlatMapJoinRhs[Q2, R2]
  )(implicit qr2: Queryable.Row[Q2, R2]): Select[Q2, R2] = {
    def rec(thing: FlatMapJoinRhs[Q2, R2], joinOns: Seq[Join]): Select[Q2, R2] = {
      thing match{
        case other: Select[Q2, R2] =>
          val simple = simpleFrom(other)
          simple.copy(from = this.from ++ simple.from)

        case other: FlatJoinMapResult[Q, Q2, R, R2] =>
          val thisTrivial = groupBy0.isEmpty
          val otherJoin = joinInfo0(
            None,
            other.from.select,
            Some(other.on),
            other.from.isTrivialJoin
          )

          copy(
            expr = other.f,
            exprPrefix = if (thisTrivial) exprPrefix else None,
            from = if (thisTrivial) from else Seq(this.subqueryRef),
            joins = (if (thisTrivial) joins else Nil) ++ Seq(otherJoin) ++ joinOns,
            where = if (thisTrivial) where else Nil,
            groupBy0 = if (thisTrivial) groupBy0 else None
          )
        case other: FlatJoinFlatMapResult[Q, Q2, R, R2] =>
          val otherJoin = joinInfo0(
            None,
            other.from.select,
            Some(other.on),
            other.from.isTrivialJoin
          )
          rec(other.f, joinOns ++ Seq(otherJoin))

      }
    }
    rec(f(expr), Nil)
  }

  def filter(f: Q => Expr[Boolean]): Select[Q, R] = {
    if (groupBy0.isEmpty) copy(where = where ++ Seq(f(expr)))
    else copy(groupBy0 = groupBy0.map(g => g.copy(having = g.having ++ Seq(f(expr)))))
  }

  def join0[Q2, R2](other: Joinable[Q2, R2], on: Option[(Q, Q2) => Expr[Boolean]])(
      implicit joinQr: Queryable.Row[Q2, R2]
  ): Select[(Q, Q2), (R, R2)] = { joinCopy(other, on, None)((_, _)) }

  protected def joinCopy[Q2, R2, Q3, R3](
      other: Joinable[Q2, R2],
      on: Option[(Q, Q2) => Expr[Boolean]],
      joinPrefix: Option[String]
  )(f: (Q, Q2) => Q3)(implicit joinQr: Queryable.Row[Q2, _], jqr: Queryable.Row[Q3, R3]) = {
    val thisTrivial = groupBy0.isEmpty
    val (otherJoin, otherSelect) = joinInfo(joinPrefix, other, on)

    copy(
      expr = f(expr, otherSelect.expr),
      exprPrefix = if (thisTrivial) exprPrefix else None,
      from = if (thisTrivial) from else Seq(this.subqueryRef),
      joins = (if (thisTrivial) joins else Nil) ++ otherJoin,
      where = if (thisTrivial) where else Nil,
      groupBy0 = if (thisTrivial) groupBy0 else None
    )
  }

  def leftJoin[Q2, R2](other: Joinable[Q2, R2])(
      on: (Q, Q2) => Expr[Boolean]
  )(implicit joinQr: Queryable.Row[Q2, R2]): Select[(Q, Nullable[Q2]), (R, Option[R2])] = {
    joinCopy(other, Some(on), Some("LEFT"))((e, o) => (e, Nullable(o)))
  }

  def rightJoin[Q2, R2](other: Joinable[Q2, R2])(
      on: (Q, Q2) => Expr[Boolean]
  )(implicit joinQr: Queryable.Row[Q2, R2]): Select[(Nullable[Q], Q2), (Option[R], R2)] = {
    joinCopy(other, Some(on), Some("RIGHT"))((e, o) => (Nullable(e), o))
  }

  def outerJoin[Q2, R2](other: Joinable[Q2, R2])(on: (Q, Q2) => Expr[Boolean])(
      implicit joinQr: Queryable.Row[Q2, R2]
  ): Select[(Nullable[Q], Nullable[Q2]), (Option[R], Option[R2])] = {
    joinCopy(other, Some(on), Some("FULL OUTER"))((e, o) => (Nullable(e), Nullable(o)))
  }

  def aggregate[E, V](f: SelectProxy[Q] => E)(implicit qr: Queryable.Row[E, V]): Aggregate[E, V] = {
    val selectProxyExpr = f(new SelectProxy[Q](expr))
    new Aggregate[E, V](
      implicit ctx => this.copy(expr = selectProxyExpr).toSqlQuery,
      selectProxyExpr
    )(qr)
  }

  def groupBy[K, V, R1, R2](groupKey: Q => K)(
      groupAggregate: SelectProxy[Q] => V
  )(implicit qrk: Queryable.Row[K, R1], qrv: Queryable.Row[V, R2]): Select[(K, V), (R1, R2)] = {
    val groupKeyValue = groupKey(expr)
    val Seq((_, groupKeyExpr)) = qrk.walk(groupKeyValue)
    val newExpr = (groupKeyValue, groupAggregate(new SelectProxy[Q](this.expr)))
    val groupByOpt = Some(GroupBy(groupKeyExpr, Nil))
    if (groupBy0.isEmpty) this.copy(expr = newExpr, groupBy0 = groupByOpt)
    else
      copy(
        expr = newExpr,
        exprPrefix = exprPrefix,
        from = Seq(this.subqueryRef),
        joins = Nil,
        where = Nil,
        groupBy0 = groupByOpt
      )
  }

  def sortBy(f: Q => Expr[_]) = {
    newCompoundSelect(this, Nil, Seq(OrderBy(f(expr), None, None)), None, None)
  }

  def asc = throw new Exception(".asc must follow .sortBy")
  def desc = throw new Exception(".desc must follow .sortBy")
  def nullsFirst = throw new Exception(".nullsFirst must follow .sortBy")
  def nullsLast = throw new Exception(".nullsLast must follow .sortBy")

  def compound0(op: String, other: Select[Q, R]) = {
    val op2 = CompoundSelect.Op(op, simpleFrom(other))
    newCompoundSelect(this, Seq(op2), Nil, None, None)
  }

  def drop(n: Int) = newCompoundSelect(this, Nil, Nil, None, Some(n))
  def take(n: Int) = newCompoundSelect(this, Nil, Nil, Some(n), None)

  def valueReader = OptionPickler.SeqLikeReader(qr.valueReader(expr), implicitly)

  def getRenderer(prevContext: Context) = new SimpleSelect.Renderer(this, prevContext)

  lazy val flattenedExpr = qr.walk(expr)
}

object SimpleSelect {

  class Renderer[Q, R](query: SimpleSelect[Q, R], prevContext: Context) extends Select.Renderer {
    val computed = Context.compute(
      prevContext,
      query.from ++ query.joins.flatMap(_.from.map(_.from)),
      None
    )

    import computed.implicitCtx

    lazy val filtersOpt = SqlStr.flatten(SqlStr.optSeq(query.where) { where =>
      sql" WHERE " + SqlStr.join(where.map(_.toSqlQuery._1), sql" AND ")
    })

    lazy val groupByOpt = SqlStr.flatten(SqlStr.opt(query.groupBy0) { groupBy =>
      val havingOpt = SqlStr.optSeq(groupBy.having) { having =>
        sql" HAVING " + SqlStr.join(having.map(_.toSqlQuery._1), sql" AND ")
      }
      sql" GROUP BY ${groupBy.expr}${havingOpt}"
    })

    lazy val jsonQueryMap = query.flattenedExpr.map { case (k, v) =>
      val str = Config.joinName(k.map(prevContext.config.columnNameMapper), prevContext.config)
      val exprId = Expr.getIdentity(v)

      (exprId, SqlStr.raw(str, Seq(exprId)))
    }.toMap

    lazy val lhsMap = jsonQueryMap

    lazy val exprsStrs = {
      FlatJson.flatten(query.flattenedExpr, implicitCtx).map { case (k, v) =>
        sql"$v as ${SqlStr.raw(implicitCtx.config.tableNameMapper(k))}"
      }
    }

    lazy val exprPrefix = SqlStr.opt(query.exprPrefix) { p => SqlStr.raw(p) + sql" " }

    def render(liveExprs: Option[Set[Expr.Identity]]) = {

      val exprStr = SqlStr.flatten(
        SqlStr.join(
          query.flattenedExpr.zip(exprsStrs).collect {
            case ((l, e), s) if liveExprs.fold(true)(_.contains(Expr.getIdentity(e))) => s
          },
          sql", "
        )
      )

      val joinOns = query.joins.map(_.from.map(_.on.map(t => SqlStr.flatten(t.toSqlQuery._1))))

      val innerLiveExprs = exprStr.referencedExprs.toSet ++ filtersOpt.referencedExprs ++
        groupByOpt.referencedExprs ++ joinOns.flatten.flatten.flatMap(_.referencedExprs)

      val joins =
        joinsToSqlStr(query.joins, computed.fromSelectables, Some(innerLiveExprs), joinOns)

      val tables = SqlStr
        .join(query.from.map(computed.fromSelectables(_)._2(Some(innerLiveExprs))), sql", ")

      sql"SELECT " + exprPrefix + exprStr + sql" FROM " + tables + joins + filtersOpt + groupByOpt
    }

    lazy val context = implicitCtx

    lazy val mappedTypes = query.flattenedExpr.map(t => Expr.getMappedType(t._2))
  }
}
