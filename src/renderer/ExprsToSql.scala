package scalasql.renderer

import scalasql.query.Expr
import scalasql.renderer.SqlStr.SqlStringSyntax
import scalasql.utils.FlatJson

object ExprsToSql {
  def apply(flattenedExpr: Seq[(List[String], Expr[_])], exprPrefix: SqlStr, context: Context) = {
    apply0(flattenedExpr, context, sql"SELECT " + exprPrefix)
  }

  def apply0(flattenedExpr: Seq[(List[String], Expr[_])], context: Context, prefix: SqlStr) = {
    FlatJson.flatten(flattenedExpr, context) match {
      case Seq((prefix, singleExpr))
          if prefix == context.config.columnLabelPrefix && singleExpr.isCompleteQuery =>
        singleExpr

      case flatQuery =>
        val exprsStr = SqlStr.join(
          flatQuery.map { case (k, v) =>
            sql"$v as ${SqlStr.raw(context.config.tableNameMapper(k))}"
          },
          sql", "
        )

        prefix + exprsStr
    }
  }

  def booleanExprs(prefix: SqlStr, exprs: Seq[Expr[_]])(implicit ctx: Context) = {
    SqlStr.optSeq(exprs.filter(!Expr.getIsLiteralTrue(_))) { having =>
      prefix + SqlStr.join(having.map(_.renderToSql._1), sql" AND ")
    }
  }
}
