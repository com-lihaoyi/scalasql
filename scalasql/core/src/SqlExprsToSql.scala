package scalasql.core

import scalasql.core.SqlStr.{Renderable, SqlStringSyntax}

object SqlExprsToSql {
  def apply(walked: Queryable.Walked, exprPrefix: SqlStr, context: Context) = {
    apply0(walked, context, sql"SELECT " + exprPrefix)
  }

  def apply0(walked: Queryable.Walked, context: Context, prefix: SqlStr) = {
    ColumnNamer.selectColumnSql(walked, context) match {
      case Seq((prefix, singleExpr))
          if prefix == context.config.columnLabelDefault && singleExpr.isCompleteQuery =>
        singleExpr

      case flatQuery =>
        val exprsStr = SqlStr.join(
          flatQuery.map { case (k, v) =>
            sql"$v AS ${SqlStr.raw(context.config.tableNameMapper(k))}"
          },
          SqlStr.commaSep
        )

        prefix + exprsStr
    }
  }

  def booleanExprs(prefix: SqlStr, exprs: Seq[Db[_]])(implicit ctx: Context) = {
    SqlStr.optSeq(exprs.filter(!Db.isLiteralTrue(_))) { having =>
      prefix + SqlStr.join(having.map(Renderable.toSql(_)), sql" AND ")
    }
  }
}
