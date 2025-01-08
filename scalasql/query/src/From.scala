package scalasql.query

import scalasql.core.{Context, Expr, ExprsToSql, LiveExprs, Queryable, SqlStr}
import scalasql.core.Context.From
import scalasql.core.SqlStr.SqlStringSyntax

/**
 * Models a SQL `FROM` clause
 */
class TableRef(val value: Table.Base) extends From {
  override def toString = s"TableRef(${Table.name(value)})"

  def fromRefPrefix(prevContext: Context) = prevContext.config.tableNameMapper(Table.name(value))

  def fromExprAliases(prevContext: Context): Seq[(Expr.Identity, SqlStr)] = Nil

  def renderSql(name: SqlStr, prevContext: Context, liveExprs: LiveExprs) = {
    val resolvedTable = Table.resolve(value)(prevContext)
    SqlStr.raw(resolvedTable + sql" " + name)
  }
}

/**
 * Models a subquery: a `SELECT`, `VALUES`, nested `WITH`, etc.
 */
class SubqueryRef(val value: SubqueryRef.Wrapped) extends From {
  def fromRefPrefix(prevContext: Context): String = "subquery"

  def fromExprAliases(prevContext: Context) = SubqueryRef.Wrapped.exprAliases(value, prevContext)

  def renderSql(name: SqlStr, prevContext: Context, liveExprs: LiveExprs) = {
    val renderSql = SubqueryRef.Wrapped.renderer(value, prevContext)
    sql"(${renderSql.render(liveExprs)}) $name"
  }
}

object SubqueryRef {

  trait Wrapped {
    protected def selectExprAliases(prevContext: Context): Seq[(Expr.Identity, SqlStr)]
    protected def selectRenderer(prevContext: Context): Wrapped.Renderer
  }
  object Wrapped {
    def exprAliases(s: Wrapped, prevContext: Context) = s.selectExprAliases(prevContext)
    def renderer(s: Wrapped, prevContext: Context) = s.selectRenderer(prevContext)

    trait Renderer {
      def render(liveExprs: LiveExprs): SqlStr
    }
  }
}

class WithCteRef(walked: Queryable.Walked) extends From {
  def fromRefPrefix(prevContext: Context) = "cte"

  def fromExprAliases(prevContext: Context) = {
    ExprsToSql.selectColumnReferences(walked, prevContext)
  }

  def renderSql(name: SqlStr, prevContext: Context, liveExprs: LiveExprs) = {
    name
  }
}
