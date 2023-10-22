package usql.query

import usql.{Column, Queryable}
import usql.renderer.{Context, SqlStr, UpdateToSql}
import usql.utils.OptionPickler

trait Update[Q, R] extends JoinOps[Update, Q, R] with Returnable[Q] with Query[Int] {
  def filter(f: Q => Expr[Boolean]): Update[Q, R]

  def set(f: (Q => (Column.ColumnExpr[_], Expr[_]))*): Update[Q, R]

  def join0[Q2, R2](other: Joinable[Q2, R2], on: Option[(Q, Q2) => Expr[Boolean]])(implicit
      joinQr: Queryable[Q2, R2]
  ): Update[(Q, Q2), (R, R2)]

  def qr: Queryable[Q, R]
  override def isExecuteUpdate = true
  override def singleRow = false
  def walk() = Nil
}

object Update {

  /**
   * Syntax reference
   *
   * https://www.postgresql.org/docs/current/sql-update.html
   */
  case class Impl[Q, R](
      expr: Q,
      table: TableRef,
      set0: Seq[(Column.ColumnExpr[_], Expr[_])],
      joins: Seq[Join],
      where: Seq[Expr[_]]
  )(implicit val qr: Queryable[Q, R]) extends Update[Q, R] {
    def filter(f: Q => Expr[Boolean]): Update.Impl[Q, R] = {
      this.copy(where = where ++ Seq(f(expr)))
    }

    def set(f: (Q => (Column.ColumnExpr[_], Expr[_]))*): Update.Impl[Q, R] = {
      this.copy(set0 = f.map(_(expr)))
    }

    def join0[Q2, R2](other: Joinable[Q2, R2], on: Option[(Q, Q2) => Expr[Boolean]])(implicit
        joinQr: Queryable[Q2, R2]
    ): Update.Impl[(Q, Q2), (R, R2)] = {
      val (otherJoin, otherSelect) = joinInfo(other, on)
      this.copy(expr = (expr, otherSelect.expr), joins = joins ++ otherJoin)
    }

    override def toSqlQuery(implicit ctx: Context): SqlStr = {
      UpdateToSql(this, ctx.tableNameMapper, ctx.columnNameMapper)
    }

    override def valueReader: OptionPickler.Reader[Int] = implicitly
  }

  def fromTable[Q, R](expr: Q, table: TableRef)(implicit qr: Queryable[Q, R]): Update.Impl[Q, R] = {
    Update.Impl(expr, table, Nil, Nil, Nil)
  }

  implicit def UpdateQueryable[Q, R]: Queryable[Update[Q, R], Int] =
    Queryable.QueryQueryable
}
