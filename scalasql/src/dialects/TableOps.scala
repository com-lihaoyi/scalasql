package scalasql.dialects

import scalasql.dialects.Dialect
import scalasql.core.{Column, Table, Sql}
import scalasql.Id
import scalasql.query.{Delete, Insert, Joinable, Select, SimpleSelect, Update}

class TableOps[V[_[_]]](val t: Table[V])(implicit dialect: Dialect)
    extends Joinable[V[Sql], V[Id]] {

  import dialect.{dialectSelf => _, _}

  protected def toFromExpr0 = {
    val ref = Table.ref(t)
    (ref, Table.metadata(t).vExpr(ref, dialect))
  }

  protected def joinableToFromExpr = {
    val (ref, expr) = toFromExpr0
    (ref, expr.asInstanceOf[V[Sql]])
  }

  protected def joinableToSelect: Select[V[Sql], V[Id]] = {
    val (ref, expr) = joinableToFromExpr
    new SimpleSelect(expr, None, Seq(ref), Nil, Nil, None)(
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
  def update(filter: V[Column] => Sql[Boolean]): Update[V[Column], V[Id]] = {
    val (ref, expr) = toFromExpr0
    new Update.Impl(expr, ref, Nil, Nil, Seq(filter(Table.metadata(t).vExpr(ref, dialect))))(
      t.containerQr2,
      dialect
    )
  }

  /**
   * Constructs a `INSERT` query
   */
  def insert: Insert[V, V[Id]] = {
    val (ref, expr) = toFromExpr0
    new Insert.Impl(expr, ref)(t.containerQr2, dialect)
  }

  /**
   * Constructs a `DELETE` query with the given [[filter]] to select the
   * rows you want to delete
   */
  def delete(filter: V[Column] => Sql[Boolean]): Delete[V[Column]] = {
    val (ref, expr) = toFromExpr0
    new Delete.Impl(expr, filter(Table.metadata(t).vExpr(ref, dialect)), ref)
  }

  protected def joinableIsTrivial = true
}
