package usql

import usql.query.{Expr, Joinable, Select, TableRef, Update}
import usql.renderer.{Context, SelectToSql, SqlStr, UpdateToSql}
import usql.renderer.SqlStr.{SqlStringSyntax, optSeq}
import usql.utils.OptionPickler

object MySqlDialect extends MySqlDialect {
  class ExprStringOps(v: Expr[String]) extends operations.ExprStringOps(v) {
    override def +(x: Expr[String]): Expr[String] = Expr { implicit ctx => usql"CONCAT($v, $x)" }

    def indexOf(x: Expr[String]): Expr[Int] = Expr { implicit ctx => usql"POSITION($x IN $v)" }
    def rpad(length: Expr[Int], fill: Expr[String]): Expr[String] = Expr { implicit ctx =>
      usql"RPAD($v, $length, $fill)"
    }
    def lpad(length: Expr[Int], fill: Expr[String]): Expr[String] = Expr { implicit ctx =>
      usql"LPAD($v, $length, $fill)"
    }
    def reverse: Expr[String] = Expr { implicit ctx => usql"REVERSE($v)" }
  }

  class TableOps[V[_[_]]](t: Table[V]) extends usql.operations.TableOps[V](t) {
    override def update: Update[V[Column.ColumnExpr], V[Val]] = {
      val ref = t.tableRef
      new Update(Update.fromTable(t.metadata.vExpr(ref), ref)(t.containerQr))
    }
  }

  class Update[Q, R](update: Update.Impl[Q, R]) extends usql.query.Update[Q, R] {
    def filter(f: Q => Expr[Boolean]): Update[Q, R] = new Update(update.filter(f))

    def set(f: Q => (Column.ColumnExpr[_], Expr[_])*): Update[Q, R] =
      new Update(update.set(f: _*))

    def join0[Q2, R2](other: Joinable[Q2, R2], on: Option[(Q, Q2) => Expr[Boolean]])(implicit
        joinQr: Queryable[Q2, R2]
    ): Update[(Q, Q2), (R, R2)] =
      new Update(update.join0(other, on))

    def expr: Q = update.expr

    def table: TableRef = update.table

    override def toSqlQuery(implicit ctx: Context): SqlStr = {
      toSqlQuery0(update, ctx.tableNameMapper, ctx.columnNameMapper)
    }

    def toSqlQuery0[Q, R](
        q: Update.Impl[Q, R],
        tableNameMapper: String => String,
        columnNameMapper: String => String
    ) = {
      val (namedFromsMap, fromSelectables, exprNaming, context) = SelectToSql.computeContext(
        tableNameMapper,
        columnNameMapper,
        q.joins.flatMap(_.from).map(_.from),
        Some(q.table),
        Map()
      )

      implicit val ctx: Context = context

      val tableName = SqlStr.raw(ctx.tableNameMapper(q.table.value.tableName))
      val updateList = q.set0.map { case (k, v) =>
        val colStr = SqlStr.raw(columnNameMapper(k.name))
        usql"$tableName.$colStr = $v"
      }
      val sets = SqlStr.join(updateList, usql", ")

      val where = SqlStr.optSeq(q.where) { where =>
        usql" WHERE " + SqlStr.join(where.map(_.toSqlQuery), usql" AND ")
      }

      val joins = optSeq(q.joins)(SelectToSql.joinsToSqlStr(_, fromSelectables))

      usql"UPDATE $tableName" + joins + usql" SET " + sets + where
    }

    def qr: Queryable[Q, R] = update.qr

    override def valueReader: OptionPickler.Reader[Int] = implicitly
  }
}
trait MySqlDialect extends Dialect {
  override implicit def ExprStringOpsConv(v: Expr[String]): MySqlDialect.ExprStringOps =
    new MySqlDialect.ExprStringOps(v)
  override implicit def TableOpsConv[V[_[_]]](t: Table[V]): usql.operations.TableOps[V] =
    new MySqlDialect.TableOps(t)
}
