package scalasql.core

import scalasql.core.SqlStr.SqlStringSyntax

/**
 * The contextual information necessary for rendering a ScalaSql query or expression
 * into a SQL string
 *
 * @param fromNaming any [[From]]/`FROM` clauses that are in scope, and the aliases those
 *                   clauses are given
 * @param exprNaming any [[Sql]]s/SQL-expressions that are present in [[fromNaming]], and
 *                   what those expressions are named in SQL
 * @param config The ScalaSql configuration
 */
trait Context {
  def fromNaming: Map[From, String]
  def exprNaming: Map[Sql.Identity, SqlStr]
  def config: Config

  def withFromNaming(fromNaming: Map[From, String]): Context
  def withExprNaming(exprNaming: Map[Sql.Identity, SqlStr]): Context
}

object Context {
  case class Impl(
      fromNaming: Map[From, String],
      exprNaming: Map[Sql.Identity, SqlStr],
      config: Config
  ) extends Context {
    def withFromNaming(fromNaming: Map[From, String]): Context = copy(fromNaming = fromNaming)

    def withExprNaming(exprNaming: Map[Sql.Identity, SqlStr]): Context =
      copy(exprNaming = exprNaming)
  }

  def compute(prevContext: Context, selectables: Seq[From], updateTable: Option[TableRef]) = {

    val prevSize = prevContext.fromNaming.size
    val newFromNaming =
      prevContext.fromNaming ++
        selectables.filter(!prevContext.fromNaming.contains(_)).zipWithIndex.toMap.map {
          case (t: TableRef, i) =>
            (t, prevContext.config.tableNameMapper(Table.tableName(t.value)) + (i + prevSize))
          case (s: SubqueryRef, i) => (s, "subquery" + (i + prevSize))
          case (s: WithCteRef, i) => (s, "cte" + (i + prevSize))
        } ++
        updateTable.map(t => t -> prevContext.config.tableNameMapper(Table.tableName(t.value)))

    val newExprNaming =
      prevContext.exprNaming ++
        selectables
          .collect { case t: SubqueryRef => t }
          .flatMap { t =>
            SelectBase
              .selectLhsMap(t.value, prevContext)
              .map { case (e, s) => (e, sql"${SqlStr.raw(newFromNaming(t), Array(e))}.$s") }
          }

    Context.Impl(newFromNaming, newExprNaming, prevContext.config)
  }

}
