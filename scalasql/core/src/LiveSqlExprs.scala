package scalasql.core

/**
 * Models a set of live [[Db]] expressions which need to be rendered;
 * [[Db]] expressions not in this set can be skipped during rendering
 * to improve the conciseness of the rendered SQL string.
 *
 * - `None` is used to indicate this is a top-level context and we want
 *   all expressions to be rendered
 *
 * - `Some(set)` indicates that only the expressions present in the `set`
 *   need to be rendered, and the rest can be elided.
 *
 * Typically downstream parts of a SQL query (e.g. the outer `SELECT`) are
 * rendered before the upstream parts (e.g. `FROM (SELECT ...)` subqueries),
 * so the [[LiveSqlExprs]] from the downstream parts can be used to decide
 * which columns to skip when rendering the upstream parts. The outermost
 * `SELECT` is rendered using [[LiveSqlExprs.none]] since we cannot know what
 * columns end up being used in the application code after the query has
 * finished running, and thus have to preserve all of them
 */
class LiveSqlExprs(values: Option[Set[Db.Identity]]) {
  def map(f: Set[Db.Identity] => Set[Db.Identity]) = new LiveSqlExprs(values.map(f))
  def isLive(e: Db.Identity) = values.fold(true)(_.contains(e))
}

object LiveSqlExprs {
  def some(v: Set[Db.Identity]) = new LiveSqlExprs(Some(v))
  def none = new LiveSqlExprs(None)
}
