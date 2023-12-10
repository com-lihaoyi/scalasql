package scalasql.core

import scalasql.core.SqlStr.SqlStringSyntax

/**
 * The contextual information necessary for rendering a ScalaSql query or expression
 * into a SQL string
 *
 * @param fromNaming any [[From]]/`FROM` clauses that are in scope, and the aliases those
 *                   clauses are given
 * @param exprNaming any [[Expr]]s/SQL-expressions that are present in [[fromNaming]], and
 *                   what those expressions are named in SQL
 * @param config The ScalaSql configuration
 */
trait Context {
  def fromNaming: Map[Context.From, String]
  def exprNaming: Map[Expr.Identity, SqlStr]
  def config: Config

  def withFromNaming(fromNaming: Map[Context.From, String]): Context
  def withExprNaming(exprNaming: Map[Expr.Identity, SqlStr]): Context
}

object Context {
  trait From {

    /**
     * What alias to name this [[From]] for better readability
     */
    def fromRefPrefix(prevContext: Context): String

    /**
     * A mapping of the [[Expr]] expressions that this [[From]] produces along
     * with their rendered [[SqlStr]]s
     */
    def fromLhsMap(prevContext: Context): Map[Expr.Identity, SqlStr]

    /**
     * How this [[From]] can be rendered into a [[SqlStr]] for embedding into
     * a larger query
     */
    def renderSql(
        name: SqlStr,
        prevContext: Context,
        liveExprs: LiveSqlExprs
    ): SqlStr
  }
  case class Impl(
      fromNaming: Map[From, String],
      exprNaming: Map[Expr.Identity, SqlStr],
      config: Config
  ) extends Context {
    def withFromNaming(fromNaming: Map[From, String]): Context = copy(fromNaming = fromNaming)

    def withExprNaming(exprNaming: Map[Expr.Identity, SqlStr]): Context =
      copy(exprNaming = exprNaming)
  }

  def compute(prevContext: Context, selectables: Seq[From], updateTable: Option[From]) = {

    val prevSize = prevContext.fromNaming.size
    val newFromNaming =
      prevContext.fromNaming ++
        selectables.filter(!prevContext.fromNaming.contains(_)).zipWithIndex.toMap.map {
          case (r, i) => (r, r.fromRefPrefix(prevContext) + (i + prevSize))
        } ++
        updateTable.map(t => t -> t.fromRefPrefix(prevContext))

    val newExprNaming =
      prevContext.exprNaming ++
        selectables
          .flatMap { t =>
            t
              .fromLhsMap(prevContext)
              .map { case (e, s) => (e, sql"${SqlStr.raw(newFromNaming(t), Array(e))}.$s") }
          }

    Context.Impl(newFromNaming, newExprNaming, prevContext.config)
  }

}
