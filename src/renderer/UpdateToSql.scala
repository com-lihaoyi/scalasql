package usql.renderer

import SqlStr.{SqlStringSyntax, optSeq}
import usql.query.Update
import usql.Queryable

object UpdateToSql {
  def apply[Q](
      q: Update[Q],
      qr: Queryable[Q, _],
      tableNameMapper: String => String,
      columnNameMapper: String => String,
      mySqlUpdateJoinSyntax: Boolean
  ) = {
    val (ctx, str) = apply0(q, tableNameMapper, columnNameMapper, mySqlUpdateJoinSyntax)
    str
  }

  def apply0[Q](
      q: Update[Q],
      tableNameMapper: String => String,
      columnNameMapper: String => String,
      mySqlUpdateJoinSyntax: Boolean
  ): (Context, SqlStr) = {
    val (namedFromsMap, fromSelectables, exprNaming, context) = SelectToSql.computeContext(
      tableNameMapper,
      columnNameMapper,
      q.joins.flatMap(_.from).map(_.from),
      Some(q.table),
      Map(),
      mySqlUpdateJoinSyntax
    )

    implicit val ctx: Context = context

    val tableName = SqlStr.raw(ctx.tableNameMapper(q.table.value.tableName))
    val updateList = q.set0.map { case (k, v) =>
      val colStr = SqlStr.raw(columnNameMapper(k.name))
      val kStr = if (mySqlUpdateJoinSyntax) usql"$tableName.$colStr" else colStr

      usql"$kStr = $v"
    }
    val sets = SqlStr.join(updateList, usql", ")

    if (mySqlUpdateJoinSyntax) {

      val where = SqlStr.optSeq(q.where) { where =>
        usql" WHERE " + SqlStr.join(where.map(_.toSqlStr), usql" AND ")
      }

      val joins = optSeq(q.joins)(SelectToSql.joinsToSqlStr(_, fromSelectables))

      (ctx, usql"UPDATE $tableName" + joins + usql" SET " + sets + where)
    }else {
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

}
