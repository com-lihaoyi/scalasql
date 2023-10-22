package usql.query

import usql.{Column, Queryable}
import usql.renderer.{Context, SqlStr, UpdateToSql}
import usql.utils.OptionPickler

trait Update[Q] extends JoinOps[Update, Q] with Returnable[Q] {
  def filter(f: Q => Expr[Boolean]): Update[Q]

  def set(f: (Q => (Column.ColumnExpr[_], Expr[_]))*): Update[Q]

  def join0[V](other: Joinable[V], on: Option[(Q, V) => Expr[Boolean]])(implicit
      joinQr: Queryable[V, _]
  ): Update[(Q, V)]

  def qr: Queryable[Q, _]
}

object Update {

  /**
   * Syntax reference
   *
   * https://www.postgresql.org/docs/current/sql-update.html
   */
  case class Impl[Q](
      expr: Q,
      table: TableRef,
      set0: Seq[(Column.ColumnExpr[_], Expr[_])],
      joins: Seq[Join],
      where: Seq[Expr[_]]
  )(implicit val qr: Queryable[Q, _]) extends Update[Q] {
    def filter(f: Q => Expr[Boolean]): Update.Impl[Q] = {
      this.copy(where = where ++ Seq(f(expr)))
    }

    def set(f: (Q => (Column.ColumnExpr[_], Expr[_]))*): Update.Impl[Q] = {
      this.copy(set0 = f.map(_(expr)))
    }

    def join0[V](other: Joinable[V], on: Option[(Q, V) => Expr[Boolean]])(implicit
        joinQr: Queryable[V, _]
    ): Update.Impl[(Q, V)] = {
      val (otherJoin, otherSelect) = joinInfo(other, on)
      this.copy(expr = (expr, otherSelect.expr), joins = joins ++ otherJoin)
    }

    override def toSqlQuery(implicit ctx: Context): SqlStr = {
      UpdateToSql(this, ctx.tableNameMapper, ctx.columnNameMapper)
    }
  }

  def fromTable[Q](expr: Q, table: TableRef)(implicit qr: Queryable[Q, _]): Update.Impl[Q] = {
    Update.Impl(expr, table, Nil, Nil, Nil)
  }

  implicit def UpdateQueryable[Q](implicit qr: Queryable[Q, _]): Queryable[Update[Q], Int] =
    new UpdateQueryable[Q]()(qr)

  class UpdateQueryable[Q](implicit qr: Queryable[Q, _]) extends Queryable[Update[Q], Int] {
    override def isExecuteUpdate = true
    def walk(ur: Update[Q]): Seq[(List[String], Expr[_])] = Nil

    override def singleRow = false

    def valueReader: OptionPickler.Reader[Int] = OptionPickler.IntReader

    override def toSqlQuery(q: Update[Q], ctx0: Context): SqlStr = {
      q.toSqlQuery(ctx0)
    }
  }
}
