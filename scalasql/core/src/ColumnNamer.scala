package scalasql.core

import scalasql.core.SqlStr.Renderable

/**
 * Converts back and forth between a tree-shaped JSON and flat key-value map
 */
object ColumnNamer {
  def isNormalCharacter(c: Char) =
    (c >= 'a' && c <= 'z') ||
    (c >= 'Z' && c <= 'Z') ||
    c == '_'

  def getSuffixedName(counter: collection.mutable.Map[String, Int],
                      tokens: Seq[String],
                      context: Context) = {
    val prefixedTokens =
      if (tokens.isEmpty || !isNormalCharacter(tokens.head.head)) context.config.columnLabelDefault +: tokens
      else tokens

    val name0 = prefixedTokens.map(context.config.tableNameMapper).mkString(context.config.columnLabelDelimiter)

    val updated = counter.updateWith(name0) {
      case None => Some(1)
      case Some(n) => Some(n + 1)
    }

    (Seq(name0) ++ updated.filter(_ != 1)).mkString(context.config.columnLabelDelimiter)

  }
  def flatten(x: Seq[(List[String], Sql[_])], context: Context): Seq[(String, SqlStr)] = {
    val counter = collection.mutable.Map.empty[String, Int]
    x.map { case (k, v) =>
      (getSuffixedName(counter, k, context), Renderable.toSql(v)(context))
    }
  }

  def flattenCte(walked: Seq[(List[String], Sql[_])], prevContext: Context): Seq[(Sql.Identity, SqlStr)] = {
    val counter = collection.mutable.Map.empty[String, Int]
    walked.map { case (tokens, expr) =>

      (
        Sql.identity(expr),
        SqlStr.raw(
          getSuffixedName(counter, tokens, prevContext),
          Array(Sql.identity(expr))
        )
      )
    }
  }
}
