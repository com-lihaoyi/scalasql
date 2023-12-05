package scalasql.query

import scalasql.core.{
  Context,
  DialectTypeMappers,
  LiveSqlExprs,
  Queryable,
  Sql,
  SqlExprsToSql,
  SqlStr,
  TypeMapper,
  WithSqlExpr
}
import scalasql.core.SqlStr.{Renderable, SqlStringSyntax, optSeq}
import scalasql.renderer.JoinsToSql

/**
 * A SQL `UPDATE` query
 */
trait Update[Q, R] extends JoinOps[Update, Q, R] with Returnable[Q] with Query[Int] {
  def filter(f: Q => Sql[Boolean]): Update[Q, R]
  def withFilter(f: Q => Sql[Boolean]): Update[Q, R] = filter(f)

  def set(f: (Q => Column.Assignment[_])*): Update[Q, R]

  def join0[Q2, R2, QF, RF](
      prefix: String,
      other: Joinable[Q2, R2],
      on: Option[(Q, Q2) => Sql[Boolean]]
  )(
      implicit ja: JoinAppend[Q, Q2, QF, RF]
  ): Update[QF, RF]

  def qr: Queryable.Row[Q, R]
  override def queryIsExecuteUpdate = true
  override def queryIsSingleRow = false
  protected def queryWalkLabels() = Nil
  protected def queryWalkExprs() = Nil
}

object Update {

  /**
   * Syntax reference
   *
   * https://www.postgresql.org/docs/current/sql-update.html
   */
  class Impl[Q, R](
      val expr: Q,
      val table: TableRef,
      val set0: Seq[Column.Assignment[_]],
      val joins: Seq[Join],
      val where: Seq[Sql[_]]
  )(implicit val qr: Queryable.Row[Q, R], dialect: DialectTypeMappers)
      extends Update[Q, R] {

    import dialect.{dialectSelf => _, _}
    protected def copy[Q, R](
        expr: Q = this.expr,
        table: TableRef = this.table,
        set0: Seq[Column.Assignment[_]] = this.set0,
        joins: Seq[Join] = this.joins,
        where: Seq[Sql[_]] = this.where
    )(implicit qr: Queryable.Row[Q, R], dialect: DialectTypeMappers): Update[Q, R] =
      new Impl(expr, table, set0, joins, where)

    def filter(f: Q => Sql[Boolean]) = { this.copy(where = where ++ Seq(f(expr))) }

    def set(f: (Q => Column.Assignment[_])*) = { this.copy(set0 = f.map(_(expr))) }

    def join0[Q2, R2, QF, RF](
        prefix: String,
        other: Joinable[Q2, R2],
        on: Option[(Q, Q2) => Sql[Boolean]]
    )(
        implicit ja: JoinAppend[Q, Q2, QF, RF]
    ) = {
      val (otherJoin, otherSelect) = joinInfo(prefix, other, on)
      this.copy(
        expr = ja.appendTuple(expr, WithSqlExpr.get(otherSelect)),
        joins = joins ++ otherJoin
      )(
        ja.qr,
        dialect
      )
    }

    protected override def renderToSql(ctx: Context): SqlStr =
      new Renderer(joins, table, set0, where, ctx).render()

    override protected def queryConstruct(args: Queryable.ResultSetIterator): Int = {
      args.get(dialect.IntType)
    }

  }

  class Renderer(
      joins0: Seq[Join],
      table: TableRef,
      set0: Seq[Column.Assignment[_]],
      where0: Seq[Sql[_]],
      prevContext: Context
  ) {
    lazy val froms = joins0.flatMap(_.from).map(_.from)
    implicit lazy val implicitCtx = Context.compute(prevContext, froms, Some(table))

    lazy val tableName =
      SqlStr.raw(implicitCtx.config.tableNameMapper(Table.name(table.value)))

    lazy val updateList = set0.map { case assign =>
      val kStr = SqlStr.raw(prevContext.config.columnNameMapper(assign.column.name))
      sql"$kStr = ${assign.value}"
    }
    lazy val sets = SqlStr.flatten(SqlStr.join(updateList, SqlStr.commaSep))

    lazy val where = SqlStr.flatten(SqlExprsToSql.booleanExprs(sql" WHERE ", fromOns ++ where0))

    lazy val liveExprs = LiveSqlExprs.some(
      sets.referencedExprs.toSet ++
        where.referencedExprs ++
        joinOns.flatten.flatten.flatMap(_.referencedExprs)
    )
    lazy val renderedFroms =
      JoinsToSql.renderFroms(froms, prevContext, implicitCtx.fromNaming, liveExprs)
    lazy val from = SqlStr.opt(joins0.headOption) { firstJoin =>
      val froms = firstJoin.from.map { jf => renderedFroms(jf.from) }
      sql" FROM " + SqlStr.join(froms, SqlStr.commaSep)
    }
    lazy val fromOns = joins0.headOption match {
      case None => Nil
      case Some(firstJoin) => firstJoin.from.flatMap(_.on)
    }

    lazy val joinOns = joins0
      .drop(1)
      .map(_.from.map(_.on.map(t => SqlStr.flatten(Renderable.toSql(t)))))

    lazy val joins = optSeq(joins0.drop(1))(JoinsToSql.joinsToSqlStr(_, renderedFroms, joinOns))

    def render() = sql"UPDATE $tableName SET " + sets + from + joins + where

  }
}
