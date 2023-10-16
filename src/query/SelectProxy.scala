package usql.query

import usql.Queryable
import usql.renderer.{Context, SqlStr}

class SelectProxy[Q](val expr: Q) extends Aggregatable[Q] {
  def queryExpr[V](f: Q => Context => SqlStr)
                  (implicit qr: Queryable[Expr[V], V]): Expr[V] = {
    Expr[V] { implicit c => f(expr)(c) }
  }
}
