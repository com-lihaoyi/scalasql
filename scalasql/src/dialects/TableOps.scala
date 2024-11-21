package scalasql.dialects

import scalasql.dialects.Dialect
import scalasql.core.{Context, Expr}
import scalasql.Sc
import scalasql.query.{Column, Delete, Insert, Joinable, Select, SimpleSelect, Table, Update}

class TableOps[V[_[_]]](val t: Table[V])(implicit dialect: Dialect)
    extends Joinable[V[Expr], V[Sc]] {

  import dialect.{dialectSelf => _}

  protected def toFromExpr0 = {
    val ref = Table.ref(t)
    (ref, Table.metadata(t).vExpr(ref, dialect))
  }

  protected def joinableToFromExpr: (Context.From, V[Expr]) = {
    val (ref, expr) = toFromExpr0
    (ref, expr.asInstanceOf[V[Expr]])
  }

  protected def joinableToSelect: Select[V[Expr], V[Sc]] = {
    val (ref, expr) = joinableToFromExpr
    new SimpleSelect(expr, None, None, false, Seq(ref), Nil, Nil, None)(
      t.containerQr,
      dialect
    )
  }

  /**
   * Constructs a `SELECT` query
   */
  def select = joinableToSelect

  /**
   * Constructs a `UPDATE` query with the given [[filter]] to select the
   * rows you want to delete
   */
  def update(filter: V[Column] => Expr[Boolean]): Update[V[Column], V[Sc]] = {
    val (ref, expr) = toFromExpr0
    new Update.Impl(expr, ref, Nil, Nil, Seq(filter(Table.metadata(t).vExpr(ref, dialect))))(
      t.containerQr2,
      dialect
    )
  }

  /**
   * Constructs a `INSERT` query
   */
  def insert: Insert[V, V[Sc]] = {
    val (ref, expr) = toFromExpr0
    new Insert.Impl(expr, ref)(t.containerQr2, dialect)
  }

  /**
   * Constructs a `DELETE` query with the given [[filter]] to select the
   * rows you want to delete
   */
  def delete(filter: V[Column] => Expr[Boolean]): Delete[V[Column]] = {
    val (ref, expr) = toFromExpr0
    new Delete.Impl(expr, filter(Table.metadata(t).vExpr(ref, dialect)), ref)
  }

}
