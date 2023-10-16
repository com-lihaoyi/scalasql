package usql.query

import usql.Queryable

trait JoinOps[C[_], Q] {

  def join[V](other: Joinable[V])
             (implicit qr: Queryable[V, _]): C[(Q, V)] = join0(other, None)

  def joinOn[V](other: Joinable[V])
               (on: (Q, V) => Expr[Boolean])
               (implicit qr: Queryable[V, _]): C[(Q, V)] = join0(other, Some(on))

  def join0[V](other: Joinable[V],
               on: Option[(Q, V) => Expr[Boolean]])
              (implicit joinQr: Queryable[V, _]): C[(Q, V)]
}