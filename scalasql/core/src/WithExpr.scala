package scalasql.core
trait WithExpr[Q] {
  protected def expr: Q
}
object WithExpr {
  def get[Q](v: WithExpr[Q]) = v.expr
}
