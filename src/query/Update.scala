package usql.query

import usql.{Column, OptionPickler, Queryable, Table}
import usql.renderer.{Context, SqlStr, UpdateToSql}

/**
 * Syntax reference
 *
 * https://www.postgresql.org/docs/current/sql-update.html
 */
case class Update[Q](
    expr: Q,
    table: TableRef,
    set0: Seq[(Column.ColumnExpr[_], Expr[_])],
    joins: Seq[Join],
    where: Seq[Expr[_]]
)(implicit val qr: Queryable[Q, _]) extends JoinOps[Update, Q] with Returnable[Q] {
  def filter(f: Q => Expr[Boolean]): Update[Q] = {
    this.copy(where = where ++ Seq(f(expr)))
  }

  def set(f: (Q => (Column.ColumnExpr[_], Expr[_]))*): Update[Q] = {
    this.copy(set0 = f.map(_(expr)))
  }

  def join0[V](other: Joinable[V], on: Option[(Q, V) => Expr[Boolean]])(implicit
      joinQr: Queryable[V, _]
  ): Update[(Q, V)] = {
    val (otherJoin, otherSelect) = joinInfo(other, on)
    this.copy(expr = (expr, otherSelect.expr), joins = joins ++ otherJoin)
  }

  override def toSqlQuery(implicit ctx: Context): SqlStr =
    Update.UpdateQueryable(qr).toSqlQuery(this, ctx)
}

object Update {
  def fromTable[Q](expr: Q, table: TableRef)(implicit qr: Queryable[Q, _]): Update[Q] = {
    Update(expr, table, Nil, Nil, Nil)
  }

  implicit def UpdateQueryable[Q](implicit qr: Queryable[Q, _]): Queryable[Update[Q], Int] =
    new UpdateQueryable[Q]()(qr)

  class UpdateQueryable[Q](implicit qr: Queryable[Q, _]) extends Queryable[Update[Q], Int] {
    override def isExecuteUpdate = true
    def walk(ur: Update[Q]): Seq[(List[String], Expr[_])] = Nil

    override def singleRow = false

    def valueReader: OptionPickler.Reader[Int] = OptionPickler.IntReader

    override def toSqlQuery(q: Update[Q], ctx0: Context): SqlStr = {
      UpdateToSql(q, qr, ctx0.tableNameMapper, ctx0.columnNameMapper)
    }
  }
}
