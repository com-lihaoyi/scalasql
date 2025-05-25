package scalasql.dialects

import scalasql.dialects.Dialect
import scalasql.core.{Context, Expr}
import scalasql.Sc
import scalasql.query.{Column, Delete, Insert, Joinable, Select, SimpleSelect, Table0, Update}

class TableOps[VExpr, VCol, VRow](val t: Table0[VExpr, VCol, VRow])(implicit dialect: Dialect)
    extends Joinable[VExpr, VRow] {

  import dialect.{dialectSelf => _}

  protected def toFromExpr0 = {
    val ref = Table0.ref(t)
    (ref, Table0.metadata(t).vExpr(ref, dialect))
  }

  protected def joinableToFromExpr: (Context.From, VExpr) = {
    val (ref, expr) = toFromExpr0
    (ref, expr.asInstanceOf[VExpr])
  }

  protected def joinableToSelect: Select[VExpr, VRow] = {
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
  def update(filter: VCol => Expr[Boolean]): Update[VCol, VRow] = {
    val (ref, expr) = toFromExpr0
    new Update.Impl(expr, ref, Nil, Nil, Seq(filter(Table0.metadata(t).vExpr(ref, dialect))))(
      t.containerQr2,
      dialect
    )
  }

  /**
   * Constructs a `INSERT` query
   */
  def insert: Insert[VExpr, VCol, VRow] = {
    val (ref, expr) = toFromExpr0
    new Insert.Impl(expr, ref)(t.containerQr2, dialect)
  }

  /**
   * Constructs a `DELETE` query with the given [[filter]] to select the
   * rows you want to delete
   */
  def delete(filter: VCol => Expr[Boolean]): Delete[VCol] = {
    val (ref, expr) = toFromExpr0
    new Delete.Impl(expr, filter(Table0.metadata(t).vExpr(ref, dialect)), ref)
  }

}
