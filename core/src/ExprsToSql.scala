package scalasql.core

import scalasql.core.SqlStr.{Renderable, SqlStringSyntax}

object ExprsToSql {
  def apply(flattenedExpr: Seq[(List[String], Sql[_])], exprPrefix: SqlStr, context: Context) = {
    apply0(flattenedExpr, context, sql"SELECT " + exprPrefix)
  }

  def apply0(flattenedExpr: Seq[(List[String], Sql[_])], context: Context, prefix: SqlStr) = {
    FlatJson.flatten(flattenedExpr, context) match {
      case Seq((prefix, singleExpr))
          if prefix == context.config.columnLabelPrefix && singleExpr.isCompleteQuery =>
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

  def booleanExprs(prefix: SqlStr, exprs: Seq[Sql[_]])(implicit ctx: Context) = {
    SqlStr.optSeq(exprs.filter(!Sql.exprIsLiteralTrue(_))) { having =>
      prefix + SqlStr.join(having.map(Renderable.renderToSql(_)), sql" AND ")
    }
  }
}
