package usql

import usql.query.{Expr, Joinable, Select, TableRef, Update}
import usql.renderer.{Context, SelectToSql, SqlStr, UpdateToSql}
import usql.renderer.SqlStr.{SqlStringSyntax, optSeq}

object MySqlDialect extends MySqlDialect{
  class ExprStringOps(v: Expr[String]) extends operations.ExprStringOps(v){
    def indexOf(x: Expr[String]): Expr[Int] = Expr { implicit ctx => usql"POSITION($x IN $v)" }
    def rpad(length: Expr[Int], fill: Expr[String]): Expr[String] = Expr { implicit ctx => usql"RPAD($v, $length, $fill)" }
    def lpad(length: Expr[Int], fill: Expr[String]): Expr[String] = Expr { implicit ctx => usql"LPAD($v, $length, $fill)" }
    def reverse: Expr[String] = Expr { implicit ctx => usql"REVERSE($v)" }
  }

  class TableOps[V[_[_]]](t: Table[V]) extends usql.operations.TableOps[V](t) {
    override def update: Update[V[Column.ColumnExpr]] = {
      val ref = t.tableRef
      new Update(Update.fromTable(t.metadata.vExpr(ref), ref)(t.containerQr))
    }
  }

  class Update[Q](update: Update.Impl[Q]) extends usql.query.Update[Q]{
    def filter(f: Q => Expr[Boolean]): Update[Q] = new Update(update.filter(f))

    def set(f: Q => (Column.ColumnExpr[_], Expr[_])*): Update[Q] =
      new Update(update.set(f:_*))

    def join0[V](other: Joinable[V],
                 on: Option[(Q, V) => Expr[Boolean]])
                (implicit joinQr: Queryable[V, _]): Update[(Q, V)] =
      new Update(update.join0(other, on))

    def expr: Q = update.expr

    def table: TableRef = update.table

    override def toSqlQuery(implicit ctx: Context): SqlStr = {
      toSqlQuery0(update, ctx.tableNameMapper, ctx.columnNameMapper)
    }

    def toSqlQuery0[Q](q: Update.Impl[Q],
                       tableNameMapper: String => String,
                       columnNameMapper: String => String) = {
      val (namedFromsMap, fromSelectables, exprNaming, context) = SelectToSql.computeContext(
        tableNameMapper,
        columnNameMapper,
        q.joins.flatMap(_.from).map(_.from),
        Some(q.table),
        Map(),
      )

      implicit val ctx: Context = context

      val tableName = SqlStr.raw(ctx.tableNameMapper(q.table.value.tableName))
      val updateList = q.set0.map { case (k, v) =>
        val colStr = SqlStr.raw(columnNameMapper(k.name))
        usql"$tableName.$colStr = $v"
      }
      val sets = SqlStr.join(updateList, usql", ")


      val where = SqlStr.optSeq(q.where) { where =>
        usql" WHERE " + SqlStr.join(where.map(_.toSqlStr), usql" AND ")
      }

      val joins = optSeq(q.joins)(SelectToSql.joinsToSqlStr(_, fromSelectables))

      usql"UPDATE $tableName" + joins + usql" SET " + sets + where
    }

    def qr: Queryable[Q, _] = update.qr
  }
}
trait MySqlDialect extends Dialect{
  override implicit def ExprStringOpsConv(v: Expr[String]): MySqlDialect.ExprStringOps = new MySqlDialect.ExprStringOps(v)
  override implicit def TableOpsConv[V[_[_]]](t: Table[V]): usql.operations.TableOps[V] = new MySqlDialect.TableOps(t)
}