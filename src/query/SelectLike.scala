package usql.query

import usql.Queryable
import usql.renderer.{Context, SqlStr}

trait SelectLike[Q] {
  def expr: Q

  def queryExpr[V](f: Context => SqlStr)
                  (implicit qr: Queryable[Expr[V], V]): Expr[V]
}
