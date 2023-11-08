package scalasql.renderer

import scalasql.Config
import scalasql.query.{Expr, From, SubqueryRef, TableRef}
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
 * @param defaultQueryableSuffix the suffix necessary for turning an expression into a valid
 *                               SQL query, e.g. some databases allow `SELECT foo` while others
 *                               require `SELECT foo FROM (VALUES (0))`
 */
trait Context{
  def fromNaming: Map[From, String]
  def exprNaming: Map[Expr.Identity, SqlStr]
  def config: Config
  def defaultQueryableSuffix: String

  def withFromNaming(fromNaming: Map[From, String]): Context
  def withExprNaming(exprNaming: Map[Expr.Identity, SqlStr]): Context
}

object Context {
  case class Impl(fromNaming: Map[From, String],
                  exprNaming: Map[Expr.Identity, SqlStr],
                  config: Config,
                  defaultQueryableSuffix: String) extends Context {
    def withFromNaming(fromNaming: Map[From, String]): Context = copy(fromNaming = fromNaming)

    def withExprNaming(exprNaming: Map[Expr.Identity, SqlStr]): Context = copy(exprNaming = exprNaming)
  }
  case class Computed(
      namedFromsMap: Map[From, String],
      fromSelectables: Map[
        From,
        (Map[Expr.Identity, SqlStr], Option[Set[Expr.Identity]] => SqlStr)
      ],
      exprNaming: Map[Expr.Identity, SqlStr],
      ctx: Context
  ) {
    implicit def implicitCtx: Context = ctx
  }

  def compute(prevContext: Context, selectables: Seq[From], updateTable: Option[TableRef]) = {
    val namedFromsMap0 = selectables.zipWithIndex.map {
      case (t: TableRef, i) => (t, prevContext.config.tableNameMapper(t.value.tableName) + i)
      case (s: SubqueryRef[_, _], i) => (s, "subquery" + i)
      case x => throw new Exception("wtf " + x)
    }.toMap

    val namedFromsMap = prevContext.fromNaming ++ namedFromsMap0 ++
      updateTable.map(t => t -> prevContext.config.tableNameMapper(t.value.tableName))

    def computeSelectable0(t: From) = t match {
      case t: TableRef => Map.empty[Expr.Identity, SqlStr]
      case t: SubqueryRef[_, _] => t.value.getRenderer(prevContext).lhsMap
    }

    def computeSelectable(t: From) = t match {
      case t: TableRef =>
        (
          Map.empty[Expr.Identity, SqlStr],
          (liveExprs: Option[Set[Expr.Identity]]) =>
            SqlStr.raw(prevContext.config.tableNameMapper(t.value.tableName)) + sql" " +
              SqlStr.raw(namedFromsMap(t))
        )

      case t: SubqueryRef[_, _] =>
        val toSqlQuery = t.value.getRenderer(prevContext)
        (
          toSqlQuery.lhsMap,
          (liveExprs: Option[Set[Expr.Identity]]) =>
            sql"(${toSqlQuery.render(liveExprs)}) ${SqlStr.raw(namedFromsMap(t))}"
        )
    }

    val fromSelectables = selectables.map(f => (f, computeSelectable(f))).toMap
    val fromSelectables0 = selectables.map(f => (f, computeSelectable0(f))).toMap

    val exprNaming = prevContext.exprNaming ++ fromSelectables0.flatMap { case (k, vs) =>
      vs.map { case (e, s) => (e, sql"${SqlStr.raw(namedFromsMap(k), Seq(e))}.$s") }
    }

    val ctx: Context = Impl(namedFromsMap, exprNaming, prevContext.config, prevContext.defaultQueryableSuffix)

    Computed(namedFromsMap, fromSelectables, exprNaming, ctx)
  }

}
