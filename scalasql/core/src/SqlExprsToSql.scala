package scalasql.core

import scalasql.core.SqlStr.{Renderable, SqlStringSyntax}

object SqlExprsToSql {
  def apply(walked: Queryable.Walked, exprPrefix: SqlStr, context: Context) = {
    apply0(walked, context, sql"SELECT " + exprPrefix)
  }

  def apply0(walked: Queryable.Walked, context: Context, prefix: SqlStr) = {
    selectColumnSql(walked, context) match {
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
    walked.map { case (k, v) => (ctx.config.renderColumnLabel(k), Renderable.toSql(v)(ctx)) }
  }

  def selectColumnReferences(walked: Queryable.Walked, ctx: Context): Seq[(Expr.Identity, SqlStr)] = {
    walked.map { case (tokens, expr) =>
      val dbId = Expr.identity(expr)
      (dbId, SqlStr.raw(ctx.config.renderColumnLabel(tokens), Array(dbId)))
    }
  }

  def booleanExprs(prefix: SqlStr, exprs: Seq[Expr[_]])(implicit ctx: Context) = {
    SqlStr.optSeq(exprs.filter(!Expr.isLiteralTrue(_))) { having =>
      prefix + SqlStr.join(having.map(Renderable.toSql(_)), sql" AND ")
    }
  }
}
