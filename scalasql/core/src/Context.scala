package scalasql.core

import scalasql.core.SqlStr.SqlStringSyntax

/**
 * The contextual information necessary for rendering a ScalaSql query or expression
 * into a SQL string
 */
trait Context {

  /**
   * Any [[From]]/`FROM` clauses that are in scope, and the aliases those clauses are given
   */
  def fromNaming: Map[Context.From, String]

  /**
   * Any [[Expr]]s/SQL-expressions that are present in [[fromNaming]], and what those
   * expressions are named in SQL
   */
  def exprNaming: Map[Expr.Identity, SqlStr]

  /**
   * The ScalaSql configuration
   */
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
     * A mapping of any aliased [[Expr]] that this [[From]] produces along
     * with their rendered [[SqlStr]]s
     */
    def fromExprAliases(prevContext: Context): Seq[(Expr.Identity, SqlStr)]

    /**
     * How this [[From]] can be rendered into a [[SqlStr]] for embedding into
     * a larger query
     */
    def renderSql(
        name: SqlStr,
        prevContext: Context,
        liveExprs: LiveExprs
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

  /**
   * Derives a new [[Context]] based on [[prevContext]] with additional [[prefixedFroms]]
   * and [[unPrefixedFroms]] added to the [[Context.fromNaming]] and [[Context.exprNaming]]
   * tables
   */
  def compute(prevContext: Context, prefixedFroms: Seq[From], unPrefixedFroms: Option[From]) = {

    val prevSize = prevContext.fromNaming.size
    val newFromNaming = Map.from(
      prevContext.fromNaming.iterator ++
        prefixedFroms.iterator.zipWithIndex.collect {
          case (r, i) if !prevContext.fromNaming.contains(r) =>
            (r, r.fromRefPrefix(prevContext) + (i + prevSize))
        } ++
        unPrefixedFroms.iterator.collect {
          case t if !prevContext.fromNaming.contains(t) =>
            t -> t.fromRefPrefix(prevContext)
        }
    )

    val newExprNaming =
      prevContext.exprNaming ++
        prefixedFroms.iterator
          .flatMap { t =>
            t
              .fromExprAliases(prevContext.withFromNaming(newFromNaming))
              .map { case (e, s) => (e, sql"${SqlStr.raw(newFromNaming(t), Array(e))}.$s") }
          }

    Context.Impl(newFromNaming, newExprNaming, prevContext.config)
  }

}
