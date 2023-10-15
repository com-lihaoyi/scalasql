package usql.renderer

import SqlStr.{SqlStringSyntax, optSeq}
import usql.query.UpdateReturning
import usql.{Queryable}

object UpdateReturningToSql {
  def apply[Q, R](q: UpdateReturning[Q, R],
                  qr: Queryable[Q, R],
                  tableNameMapper: String => String,
                  columnNameMapper: String => String) = {
    val (namedFromsMap, fromSelectables, exprNaming, context) = SelectToSql.computeContext(
      tableNameMapper,
      columnNameMapper,
      q.update.joins.flatMap(_.from).map(_.from),
      Some(q.update.table)
    )

    implicit val ctx: Context = context

    val tableName = SqlStr.raw(ctx.tableNameMapper(q.update.table.value.tableName))
    val updateList = q.update.set0.map { case (k, v) =>
      val setLhsCtxt = new Context(
        ctx.fromNaming + (q.update.table -> ""),
        ctx.exprNaming,
        ctx.tableNameMapper,
        ctx.columnNameMapper
      )

      val kStr = k.toSqlExpr(setLhsCtxt)
      usql"$kStr = $v"
    }
    val sets = SqlStr.join(updateList, usql", ")

    val (from, fromOns) = q.update.joins.headOption match {
      case None => (usql"", Nil)
      case Some(firstJoin) =>
        val (froms, ons) = firstJoin.from.map { jf => (fromSelectables(jf.from)._2, jf.on) }.unzip
        (usql" FROM " + SqlStr.join(froms, usql", "), ons.flatten)
    }

    val where = SqlStr.optSeq(fromOns ++ q.update.where) { where =>
      usql" WHERE " + SqlStr.join(where.map(_.toSqlExpr), usql" AND ")
    }


    val joins = optSeq(q.update.joins.drop(1))(SelectToSql.joinsToSqlStr(_, fromSelectables))

    val (flattenedExpr, exprStr) = SelectToSql.sqlExprsStr0(q.returning, qr, ctx, usql"")

    val returning = usql" RETURNING " + exprStr
    val res = usql"UPDATE $tableName SET " + sets + from + joins + where + returning
    val flattened = SqlStr.flatten(res)
    flattened
  }
}
