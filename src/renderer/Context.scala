package scalasql.renderer

import scalasql.Config
import scalasql.query.{Expr, From, Select, SubqueryRef, TableRef}
import scalasql.renderer.SqlStr.SqlStringSyntax

case class Context(
    fromNaming: Map[From, String],
    exprNaming: Map[Expr.Identity, SqlStr],
    config: Config,
    defaultQueryableSuffix: String
)

object Context {
  case class Computed(
      namedFromsMap: Map[From, String],
      fromSelectables: Map[From, (Map[Expr.Identity, SqlStr], SqlStr)],
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

    def computeSelectable(t: From) = t match {
      case t: TableRef => (
          Map.empty[Expr.Identity, SqlStr],
          SqlStr.raw(prevContext.config.tableNameMapper(t.value.tableName)) + sql" " +
            SqlStr.raw(namedFromsMap(t))
        )

      case t: SubqueryRef[_, _] =>
        val (subNameMapping, sqlStr, _, _) = t.value.toSqlQuery0(prevContext)
        (subNameMapping, sql"($sqlStr) ${SqlStr.raw(namedFromsMap(t))}")
    }

    val fromSelectables = selectables.map(f => (f, computeSelectable(f))).toMap

    val exprNaming = fromSelectables.flatMap { case (k, vs) =>
      vs._1.map { case (e, s) => (e, sql"${SqlStr.raw(namedFromsMap(k))}.$s") }
    }

    val ctx: Context = prevContext.copy(fromNaming = namedFromsMap, exprNaming = exprNaming)

    Computed(namedFromsMap, fromSelectables, exprNaming, ctx)
  }

}
