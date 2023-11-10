package scalasql.query

import scalasql.renderer.SqlStr.{Renderable, SqlStringSyntax, optSeq}
import scalasql.{Column, MappedType, Queryable}
import scalasql.renderer.{Context, ExprsToSql, JoinsToSql, SqlStr}
import scalasql.utils.OptionPickler

/**
 * A SQL `UPDATE` query
 */
trait Update[Q, R] extends JoinOps[Update, Q, R] with Returnable[Q] with Query[Int] {
  def filter(f: Q => Expr[Boolean]): Update[Q, R]
  def withFilter(f: Q => Expr[Boolean]): Update[Q, R] = filter(f)

  def set(f: (Q => Column.Assignment[_])*): Update[Q, R]

  def join0[Q2, R2](
      prefix: Option[String],
      other: Joinable[Q2, R2],
      on: Option[(Q, Q2) => Expr[Boolean]]
  )(
      implicit joinQr: Queryable.Row[Q2, R2]
  ): Update[(Q, Q2), (R, R2)]

  def qr: Queryable.Row[Q, R]
  override def queryIsExecuteUpdate = true
  override def queryIsSingleRow = false
  def queryWalkExprs() = Nil
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
      val where: Seq[Expr[_]]
  )(implicit val qr: Queryable.Row[Q, R])
      extends Update[Q, R] {
    protected def copy[Q, R](
        expr: Q = this.expr,
        table: TableRef = this.table,
        set0: Seq[Column.Assignment[_]] = this.set0,
        joins: Seq[Join] = this.joins,
        where: Seq[Expr[_]] = this.where
    )(implicit qr: Queryable.Row[Q, R]): Update[Q, R] = new Impl(expr, table, set0, joins, where)

    def filter(f: Q => Expr[Boolean]) = { this.copy(where = where ++ Seq(f(expr))) }

    def set(f: (Q => Column.Assignment[_])*) = { this.copy(set0 = f.map(_(expr))) }

    def join0[Q2, R2](
        prefix: Option[String],
        other: Joinable[Q2, R2],
        on: Option[(Q, Q2) => Expr[Boolean]]
    )(
        implicit joinQr: Queryable.Row[Q2, R2]
    ) = {
      val (otherJoin, otherSelect) = joinInfo(prefix, other, on)
      this.copy(expr = (expr, otherSelect.expr), joins = joins ++ otherJoin)
    }

    protected override def renderToSql(implicit ctx: Context): (SqlStr, Seq[MappedType[_]]) =
      new Renderer(joins, table, set0, where, ctx).render()

    protected override def queryValueReader: OptionPickler.Reader[Int] = implicitly

  }

  class Renderer(
      joins0: Seq[Join],
      table: TableRef,
      set0: Seq[Column.Assignment[_]],
      where0: Seq[Expr[_]],
      prevContext: Context
  ) {
    val computed = Context.compute(prevContext, joins0.flatMap(_.from).map(_.from), Some(table))

    import computed.implicitCtx

    lazy val tableName = SqlStr.raw(implicitCtx.config.tableNameMapper(table.value.tableName))

    lazy val updateList = set0.map { case assign =>
      val kStr = SqlStr.raw(prevContext.config.columnNameMapper(assign.column.name))
      sql"$kStr = ${assign.value}"
    }
    lazy val sets = SqlStr.flatten(SqlStr.join(updateList, sql", "))

    lazy val liveExprs = sets.referencedExprs.toSet ++ SqlStr.flatten(where).referencedExprs
    lazy val from = SqlStr.opt(joins0.headOption) { firstJoin =>
      val froms = firstJoin.from.map { jf => computed.fromSelectables(jf.from)._2 }
      sql" FROM " + SqlStr.join(froms.map(_(Some(liveExprs))), sql", ")
    }
    lazy val fromOns = joins0.headOption match {
      case None => Nil
      case Some(firstJoin) => firstJoin.from.flatMap(_.on)
    }

    lazy val where = ExprsToSql.booleanExprs(sql" WHERE ", fromOns ++ where0)

    lazy val joinOns = joins0
      .drop(1)
      .map(_.from.map(_.on.map(t => SqlStr.flatten(Renderable.renderToSql(t)._1))))

    lazy val joins = optSeq(joins0.drop(1))(
      JoinsToSql.joinsToSqlStr(_, computed.fromSelectables, Some(liveExprs), joinOns)
    )

    def render() =
      (sql"UPDATE $tableName SET " + sets + from + joins + where, Seq(MappedType.IntType))

  }
}
