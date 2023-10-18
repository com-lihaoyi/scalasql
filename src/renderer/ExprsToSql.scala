package usql.renderer

import usql.FlatJson
import usql.query.Expr
import usql.renderer.SqlStr.SqlStringSyntax

object ExprsToSql {
  def apply(flattenedExpr: Seq[(List[String], Expr[_])], exprPrefix: SqlStr, context: Context) = {
    apply0(flattenedExpr, context, usql"SELECT " + exprPrefix)
  }

  def apply0(flattenedExpr: Seq[(List[String], Expr[_])], context: Context, prefix: SqlStr) = {
    FlatJson.flatten(flattenedExpr, context) match {
      case Seq((FlatJson.basePrefix, singleExpr)) if singleExpr.isCompleteQuery =>
        (flattenedExpr, singleExpr)

      case flatQuery =>

        val exprsStr = SqlStr.join(
          flatQuery.map {
            case (k, v) => usql"$v as ${SqlStr.raw(context.tableNameMapper(k))}"
          },
          usql", "
        )

        (flattenedExpr, prefix + exprsStr)
    }
  }
}
