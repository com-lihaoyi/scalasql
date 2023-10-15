package usql

import usql.Query.{From, Join}

/**
 * Syntax reference
 *
 * https://www.postgresql.org/docs/current/sql-update.html
 */
case class Update[Q](expr: Q,
                     table: Query.TableRef,
                     set0: Seq[(Expr[_], Expr[_])],
                     from: Seq[From],
                     joins: Seq[Join],
                     where: Seq[Expr[_]])
                    (implicit val qr: Queryable[Q, _]){
  def filter(f: Q => Expr[Boolean]): Update[Q] = {
    this.copy(where = where ++ Seq(f(expr)))
  }
  def set(f: (Q => (Expr[_], Expr[_]))*): Update[Q] = {
    this.copy(set0 = f.map(_(expr)))
  }

  def returning[Q2, R](f: Q => Q2)(implicit qr: Queryable[Q2, R]): UpdateReturning[Q2, R] = {
    UpdateReturning(this, f(expr))
  }
}

object Update{
  def fromTable[Q](expr: Q, table: Query.TableRef)(implicit qr: Queryable[Q, _]): Update[Q] = {
    Update(expr, table, Nil, Nil, Nil, Nil)
  }
}

case class UpdateReturning[Q, R](update: Update[_], returning: Q)(implicit val qr: Queryable[Q, R]) {
}