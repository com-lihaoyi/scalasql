package scalasql.renderer

import scalasql.Config
import scalasql.query.{Expr, From, Select, SubqueryRef, TableRef}
import scalasql.renderer.SqlStr.SqlStringSyntax

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
  def fromNaming: Map[From, String]
  def exprNaming: Map[Expr.Identity, SqlStr]
  def config: Config

  def withFromNaming(fromNaming: Map[From, String]): Context
  def withExprNaming(exprNaming: Map[Expr.Identity, SqlStr]): Context
}

object Context {
  case class Impl(
      fromNaming: Map[From, String],
      exprNaming: Map[Expr.Identity, SqlStr],
      config: Config
  ) extends Context {
    def withFromNaming(fromNaming: Map[From, String]): Context = copy(fromNaming = fromNaming)

    def withExprNaming(exprNaming: Map[Expr.Identity, SqlStr]): Context =
      copy(exprNaming = exprNaming)
  }

  def compute(prevContext: Context, selectables: Seq[From], updateTable: Option[TableRef]) = {

    val prevSize = prevContext.fromNaming.size
    val namedFromsMap =
      prevContext.fromNaming ++
        selectables.zipWithIndex.map {
          case (t: TableRef, i) =>
            (t, prevContext.config.tableNameMapper(t.value.tableName) + (i + prevSize))
          case (s: SubqueryRef[_, _], i) => (s, "subquery" + (i + prevSize))
        }.toMap ++
        updateTable.map(t => t -> prevContext.config.tableNameMapper(t.value.tableName))

    val exprNaming =
      prevContext.exprNaming ++
        selectables.collect { case t: SubqueryRef[_, _] =>
          Select.getLhsMap(t.value, prevContext)
            .map { case (e, s) => (e, sql"${SqlStr.raw(namedFromsMap(t), Seq(e))}.$s") }
        }.flatten

    Context.Impl(
      namedFromsMap,
      exprNaming,
      prevContext.config
    )
  }

}
