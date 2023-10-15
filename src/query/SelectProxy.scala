package usql.query

import usql.Queryable
import usql.renderer.{Context, SqlStr}

class SelectProxy[Q](val expr: Q) extends SelectLike[Q] {
  def queryExpr[V](f: Context => SqlStr)
                  (implicit qr: Queryable[Expr[V], V]): Expr[V] = {
    Expr[V] { implicit c => f(c) }
  }
}
