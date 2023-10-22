package usql.renderer

import SqlStr.{SqlStringSyntax, optSeq}
import usql.query.Update
import usql.Queryable

object UpdateToSql {
  def apply[Q](
      q: Update.Impl[Q],
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

    usql"UPDATE $tableName SET " + sets + from + joins + where

  }
}
