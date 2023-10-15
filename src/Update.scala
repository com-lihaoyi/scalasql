package usql

import usql.Select.{From, Join}

/**
 * Syntax reference
 *
 * https://www.postgresql.org/docs/current/sql-update.html
 */
case class Update[Q](expr: Q,
                     table: Select.TableRef,
                     set0: Seq[(Expr[_], Expr[_])],
                     joins: Seq[Join],
                     where: Seq[Expr[_]])
                    (implicit val qr: Queryable[Q, _]){
  def filter(f: Q => Expr[Boolean]): Update[Q] = {
    this.copy(where = where ++ Seq(f(expr)))
  }
  def set(f: (Q => (Expr[_], Expr[_]))*): Update[Q] = {
    this.copy(set0 = f.map(_(expr)))
  }

  def joinOn[V](other: Select[V])
               (on: (Q, V) => Expr[Boolean])
               (implicit qr: Queryable[V, _]): Update[(Q, V)] = join0(other, Some(on))

  def join0[V](other: Select[V],
               on: Option[(Q, V) => Expr[Boolean]])
              (implicit joinQr: Queryable[V, _]): Update[(Q, V)] = {
    val otherTrivial = other.simple(other.groupBy0, other.orderBy, other.limit, other.offset)
    Update(
      (expr, other.expr),
      table = table,
      set0 = set0,
      joins =
        joins ++
        (if (otherTrivial) Seq(Join(None, other.from.map(Select.JoinFrom(_, on.map(_(expr, other.expr))))))
        else Seq(Join(None, Seq(Select.JoinFrom(new Select.SubqueryRef(other, joinQr), on.map(_(expr, other.expr))))))),
      where = where
    )

  }

  def returning[Q2, R](f: Q => Q2)(implicit qr: Queryable[Q2, R]): UpdateReturning[Q2, R] = {
    UpdateReturning(this, f(expr))
  }
}

object Update{
  def fromTable[Q](expr: Q, table: Select.TableRef)(implicit qr: Queryable[Q, _]): Update[Q] = {
    Update(expr, table, Nil, Nil, Nil)
  }
}

case class UpdateReturning[Q, R](update: Update[_], returning: Q)(implicit val qr: Queryable[Q, R]) {
}