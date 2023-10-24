package scalasql.dialects

import scalasql._
import scalasql.query.{Expr, Joinable, TableRef, Update}
import scalasql.renderer.SqlStr.{SqlStringSyntax, optSeq}
import scalasql.renderer.{Context, SelectToSql, SqlStr}
import scalasql.utils.OptionPickler

object MySqlDialect extends MySqlDialect {
  class ExprStringOps(val v: Expr[String]) extends operations.ExprStringOps(v) with PadOps {
    override def +(x: Expr[String]): Expr[String] = Expr { implicit ctx => sql"CONCAT($v, $x)" }

    def indexOf(x: Expr[String]): Expr[Int] = Expr { implicit ctx => sql"POSITION($x IN $v)" }
    def reverse: Expr[String] = Expr { implicit ctx => sql"REVERSE($v)" }
  }

  class TableOps[V[_[_]]](t: Table[V]) extends scalasql.operations.TableOps[V](t) {
    override def update: Update[V[Column.ColumnExpr], V[Val]] = {
      val ref = t.tableRef
      new Update(Update.fromTable(t.metadata.vExpr(ref), ref)(t.containerQr))
    }
  }

  class Update[Q, R](update: Update.Impl[Q, R]) extends scalasql.query.Update[Q, R] {
    def filter(f: Q => Expr[Boolean]): Update[Q, R] = new Update(update.filter(f))

    def set(f: Q => (Column.ColumnExpr[_], Expr[_])*): Update[Q, R] =
      new Update(update.set(f: _*))

    def join0[Q2, R2](other: Joinable[Q2, R2], on: Option[(Q, Q2) => Expr[Boolean]])(implicit
        joinQr: Queryable[Q2, R2]
    ): Update[(Q, Q2), (R, R2)] =
      new Update(update.join0(other, on))

    def expr: Q = update.expr

    def table: TableRef = update.table

    override def toSqlQuery(implicit ctx: Context): (SqlStr, Seq[MappedType[_]]) = {
      toSqlQuery0(update, ctx)
    }

    def toSqlQuery0[Q, R](
        q: Update.Impl[Q, R],
        prevContext: Context
    ): (SqlStr, Seq[MappedType[_]]) = {
      val (namedFromsMap, fromSelectables, exprNaming, context) = SelectToSql.computeContext(
        prevContext,
        q.joins.flatMap(_.from).map(_.from),
        Some(q.table)
      )

      implicit val ctx: Context = context

      val tableName = SqlStr.raw(ctx.tableNameMapper(q.table.value.tableName))
      val updateList = q.set0.map { case (k, v) =>
        val colStr = SqlStr.raw(prevContext.columnNameMapper(k.name))
        sql"$tableName.$colStr = $v"
      }
      val sets = SqlStr.join(updateList, sql", ")

      val where = SqlStr.optSeq(q.where) { where =>
        sql" WHERE " + SqlStr.join(where.map(_.toSqlQuery._1), sql" AND ")
      }

      val joins = optSeq(q.joins)(SelectToSql.joinsToSqlStr(_, fromSelectables))

      (sql"UPDATE $tableName" + joins + sql" SET " + sets + where, Nil)
    }

    def qr: Queryable[Q, R] = update.qr

    override def valueReader: OptionPickler.Reader[Int] = implicitly
  }
}
trait MySqlDialect extends Dialect {
  override implicit def ExprStringOpsConv(v: Expr[String]): MySqlDialect.ExprStringOps =
    new MySqlDialect.ExprStringOps(v)
  override implicit def TableOpsConv[V[_[_]]](t: Table[V]): scalasql.operations.TableOps[V] =
    new MySqlDialect.TableOps(t)
}
