package scalasql.query

import scalasql.core.{Context, LiveSqlExprs, Expr, SqlStr}

trait SelectBase {
  protected def selectColumnExprs(prevContext: Context): Map[Expr.Identity, SqlStr]
  protected def selectRenderer(prevContext: Context): SelectBase.Renderer
}
object SelectBase {
  def columnExprs(s: SelectBase, prevContext: Context) = s.selectColumnExprs(prevContext)
  def renderer(s: SelectBase, prevContext: Context) = s.selectRenderer(prevContext)

  trait Renderer {
    def render(liveExprs: LiveSqlExprs): SqlStr
  }

}
