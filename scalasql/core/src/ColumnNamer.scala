package scalasql.core

import scalasql.core.SqlStr.Renderable

/**
 * Provides pretty column labels for your SELECT clauses. The mapping is
 * are unique, concise, and readable, but does not need to be reversible
 * since we do not use the names when re-constructing the final query
 * return values.
 */
object ColumnNamer {
  def isNormalCharacter(c: Char) = (c >= 'a' && c <= 'z') || (c >= 'Z' && c <= 'Z') || c == '_'

  def getSuffixedName(
      tokens: Seq[String],
      context: Context
  ) = {
    val prefixedTokens =
      if (tokens.isEmpty || !isNormalCharacter(tokens.head.head))
        context.config.columnLabelDefault +: tokens
      else tokens

    prefixedTokens
      .map(context.config.tableNameMapper)
      .mkString(context.config.columnLabelDelimiter)
  }

  def selectColumnSql(walked: Queryable.Walked, ctx: Context): Seq[(String, SqlStr)] = {
    walked.map { case (k, v) => (getSuffixedName(k, ctx), Renderable.toSql(v)(ctx)) }
  }

  def selectColumnReferences(walked: Queryable.Walked, ctx: Context): Seq[(Db.Identity, SqlStr)] = {
    walked.map { case (tokens, expr) =>
      val dbId = Db.identity(expr)
      (dbId, SqlStr.raw(getSuffixedName(tokens, ctx), Array(dbId)))
    }
  }
}
