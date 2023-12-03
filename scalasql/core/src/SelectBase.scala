package scalasql.core

trait SelectBase {
  protected def selectLhsMap(prevContext: Context): Map[Sql.Identity, SqlStr]
  protected def selectRenderer(prevContext: Context): SelectBase.Renderer
}
object SelectBase {
  def lhsMap(s: SelectBase, prevContext: Context) = s.selectLhsMap(prevContext)
  def renderer(s: SelectBase, prevContext: Context) = s.selectRenderer(prevContext)

  trait Renderer {
    def render(liveExprs: Option[Set[Sql.Identity]]): SqlStr
  }

}
