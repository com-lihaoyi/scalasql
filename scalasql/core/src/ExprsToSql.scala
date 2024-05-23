package scalasql.core

import scalasql.core.SqlStr.{Renderable, SqlStringSyntax}

object ExprsToSql {

  def apply(walked: Queryable.Walked, context: Context, prefix: SqlStr): SqlStr = {
    selectColumnSql(walked, context) match {
      // Aggregate operators return expressions that are actually entire queries.
      // We thus check to avoid redundantly wrapping them in another `SELECT`, and
      // instead return them unchanged
      case Seq((prefix, singleExpr))
          if prefix == context.config.renderColumnLabel(Nil) && singleExpr.isCompleteQuery =>
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

  def selectColumnSql(walked: Queryable.Walked, ctx: Context): Seq[(String, SqlStr)] = {
    walked.map { case (k, v) => (ctx.config.renderColumnLabel(k), Renderable.renderSql(v)(ctx)) }
  }

  def selectColumnReferences(
      walked: Queryable.Walked,
      ctx: Context
  ): Seq[(Expr.Identity, SqlStr)] = {
    walked.map { case (tokens, expr) =>
      val dbId = Expr.identity(expr)
      (dbId, SqlStr.raw(ctx.config.renderColumnLabel(tokens), Array(dbId)))
    }
  }

  def booleanExprs(prefix: SqlStr, exprs: Seq[Expr[?]])(implicit ctx: Context) = {
    SqlStr.optSeq(exprs.filter(!Expr.isLiteralTrue(_))) { having =>
      prefix + SqlStr.join(having.map(Renderable.renderSql(_)), sql" AND ")
    }
  }
}
