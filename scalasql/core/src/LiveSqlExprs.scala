package scalasql.core

/**
 * Models a set of live [[Expr]] expressions which need to be rendered;
 * [[Expr]] expressions not in this set can be skipped during rendering
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
 * so the [[LiveExprs]] from the downstream parts can be used to decide
 * which columns to skip when rendering the upstream parts. The outermost
 * `SELECT` is rendered using [[LiveExprs.none]] since we cannot know what
 * columns end up being used in the application code after the query has
 * finished running, and thus have to preserve all of them
 */
class LiveExprs(val values: Option[Set[Expr.Identity]]) {
  def map(f: Set[Expr.Identity] => Set[Expr.Identity]) = new LiveExprs(values.map(f))
  def isLive(e: Expr.Identity) = values.fold(true)(_.contains(e))
}

object LiveExprs {
  def some(v: Set[Expr.Identity]) = new LiveExprs(Some(v))
  def none = new LiveExprs(None)
}
