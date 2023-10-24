package scalasql.renderer

import SqlStr.{SqlStringSyntax, optSeq}
import scalasql.query.Update
import scalasql.{MappedType, Queryable}

object UpdateToSql {
  def apply[Q, R](
      q: Update.Impl[Q, R],
      prevContext: Context
  ) = {
    val (namedFromsMap, fromSelectables, exprNaming, context) = SelectToSql.computeContext(
      prevContext,
      q.joins.flatMap(_.from).map(_.from),
      Some(q.table)
    )

    implicit val ctx: Context = context

    val tableName = SqlStr.raw(ctx.tableNameMapper(q.table.value.tableName))
    val updateList = q.set0.map { case (k, v) =>
      val kStr = SqlStr.raw(prevContext.columnNameMapper(k.name))
      sql"$kStr = $v"
    }
    val sets = SqlStr.join(updateList, sql", ")

    val (from, fromOns) = q.joins.headOption match {
      case None => (sql"", Nil)
      case Some(firstJoin) =>
        val (froms, ons) = firstJoin.from.map { jf => (fromSelectables(jf.from)._2, jf.on) }.unzip
        (sql" FROM " + SqlStr.join(froms, sql", "), ons.flatten)
    }

    val where = SqlStr.optSeq(fromOns ++ q.where) { where =>
      sql" WHERE " + SqlStr.join(where.map(_.toSqlQuery._1), sql" AND ")
    }

    val joins = optSeq(q.joins.drop(1))(SelectToSql.joinsToSqlStr(_, fromSelectables))

    (sql"UPDATE $tableName SET " + sets + from + joins + where, Seq(MappedType.IntType))

  }
}
