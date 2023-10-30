package scalasql.query

import scalasql.renderer.SqlStr.{SqlStringSyntax, optSeq}
import scalasql.{Column, MappedType, Queryable}
import scalasql.renderer.{Context, SelectToSql, SqlStr}
import scalasql.utils.OptionPickler

trait Update[Q, R] extends JoinOps[Update, Q, R] with Returnable[Q] with Query[Int] {
  def filter(f: Q => Expr[Boolean]): Update[Q, R]

  def set(f: (Q => (Column.ColumnExpr[_], Expr[_]))*): Update[Q, R]

  def join0[Q2, R2](other: Joinable[Q2, R2], on: Option[(Q, Q2) => Expr[Boolean]])(
      implicit joinQr: Queryable[Q2, R2]
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
  class Impl[Q, R](
      val expr: Q,
      val table: TableRef,
      val set0: Seq[(Column.ColumnExpr[_], Expr[_])],
      val joins: Seq[Join],
      val where: Seq[Expr[_]]
  )(implicit val qr: Queryable[Q, R])
      extends Update[Q, R] {
    def copy[Q, R](
                    expr: Q = this.expr,
                    table: TableRef = this.table,
                    set0: Seq[(Column.ColumnExpr[_], Expr[_])] = this.set0,
                    joins: Seq[Join] = this.joins,
                    where: Seq[Expr[_]] = this.where)
                  (implicit qr: Queryable[Q, R]): Update[Q, R] = new Impl(expr,
      table,
      set0,
      joins,
      where
    )


    def filter(f: Q => Expr[Boolean]) = {
      this.copy(where = where ++ Seq(f(expr)))
    }

    def set(f: (Q => (Column.ColumnExpr[_], Expr[_]))*) = {
      this.copy(set0 = f.map(_(expr)))
    }

    def join0[Q2, R2](other: Joinable[Q2, R2], on: Option[(Q, Q2) => Expr[Boolean]])(
        implicit joinQr: Queryable[Q2, R2]
    ) = {
      val (otherJoin, otherSelect) = joinInfo(other, on)
      this.copy(expr = (expr, otherSelect.expr), joins = joins ++ otherJoin)
    }

    override def toSqlQuery(implicit ctx: Context): (SqlStr, Seq[MappedType[_]]) =
      toSqlStr(joins, table, set0, where, ctx)

    override def valueReader: OptionPickler.Reader[Int] = implicitly
  }

  def toSqlStr(
      joins0: Seq[Join],
      table: TableRef,
      set0: Seq[(Column.ColumnExpr[_], Expr[_])],
      where0: Seq[Expr[_]],
      prevContext: Context
  ) = {
    val computed = Context.compute(prevContext, joins0.flatMap(_.from).map(_.from), Some(table))

    import computed.implicitCtx

    val tableName = SqlStr.raw(implicitCtx.config.tableNameMapper(table.value.tableName))
    val updateList = set0.map { case (k, v) =>
      val kStr = SqlStr.raw(prevContext.config.columnNameMapper(k.name))
      sql"$kStr = $v"
    }
    val sets = SqlStr.join(updateList, sql", ")

    val (from, fromOns) = joins0.headOption match {
      case None => (sql"", Nil)
      case Some(firstJoin) =>
        val (froms, ons) = firstJoin.from.map { jf =>
          (computed.fromSelectables(jf.from)._2, jf.on)
        }.unzip
        (sql" FROM " + SqlStr.join(froms, sql", "), ons.flatten)
    }

    val where = SqlStr.optSeq(fromOns ++ where0) { where =>
      sql" WHERE " + SqlStr.join(where.map(_.toSqlQuery._1), sql" AND ")
    }

    val joins = optSeq(joins0.drop(1))(SelectToSql.joinsToSqlStr(_, computed.fromSelectables))

    (sql"UPDATE $tableName SET " + sets + from + joins + where, Seq(MappedType.IntType))

  }
}
