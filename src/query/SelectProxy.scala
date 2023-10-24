package scalasql.query

import scalasql.{MappedType, Queryable}
import scalasql.renderer.{Context, SqlStr}

class SelectProxy[Q](val expr: Q) extends Aggregatable[Q] {
  def queryExpr[V: MappedType](f: Q => Context => SqlStr)(implicit
      qr: Queryable[Expr[V], V]
  ): Expr[V] = {
    Expr[V] { implicit c => f(expr)(c) }
  }
}
