package usql.renderer

import SqlStr.{SqlStringSyntax, optSeq}
import usql.query.{Update, UpdateReturning}
import usql.Queryable

object UpdateToSql {
  def apply[Q](q: Update[Q],
               qr: Queryable[Q, _],
               tableNameMapper: String => String,
               columnNameMapper: String => String) = {
    val (ctx, str) = apply0(q, tableNameMapper, columnNameMapper)
    str
  }

  def returning[Q, R](q: UpdateReturning[Q, R],
                  qr: Queryable[Q, R],
                  tableNameMapper: String => String,
                  columnNameMapper: String => String) = {
    val (ctx, str) = apply0(q.update, tableNameMapper, columnNameMapper)
    val (flattenedExpr, exprStr) = SelectToSql.sqlExprsStr0(q.returning, qr, ctx, usql"")

    val returning = usql" RETURNING " + exprStr
    str + returning
  }

  def apply0[Q](q: Update[Q],
                tableNameMapper: String => String,
                columnNameMapper: String => String) = {
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
      val kStr = SqlStr.raw(columnNameMapper(k.name))
      usql"$kStr = $v"
    }
    val sets = SqlStr.join(updateList, usql", ")

    val (from, fromOns) = q.joins.headOption match {
      case None => (usql"", Nil)
      case Some(firstJoin) =>
        val (froms, ons) = firstJoin.from.map { jf => (fromSelectables(jf.from)._2, jf.on) }.unzip
        (usql" FROM " + SqlStr.join(froms, usql", "), ons.flatten)
    }

    val where = SqlStr.optSeq(fromOns ++ q.where) { where =>
      usql" WHERE " + SqlStr.join(where.map(_.toSqlStr), usql" AND ")
    }


    val joins = optSeq(q.joins.drop(1))(SelectToSql.joinsToSqlStr(_, fromSelectables))

    (ctx, usql"UPDATE $tableName SET " + sets + from + joins + where)
  }


}
