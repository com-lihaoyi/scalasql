package scalasql.core
trait WithSqlExpr[Q] {
  protected def expr: Q
}
object WithSqlExpr {
  def get[Q](v: WithSqlExpr[Q]) = v.expr
}
