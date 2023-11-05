package scalasql.operations

import scalasql.{Column, Id, Table}
import scalasql.query.{Delete, Expr, Insert, Joinable, Select, SimpleSelect, Update}

class TableOps[V[_[_]]](t: Table[V]) extends Joinable[V[Expr], V[Id]] {
  /**
   * Constructs a `SELECT` query
   */
  def select: Select[V[Expr], V[Id]] = {
    val ref = t.tableRef
    new SimpleSelect(t.metadata.vExpr(ref).asInstanceOf[V[Expr]], None, Seq(ref), Nil, Nil, None)(
      t.containerQr
    )
  }

  /**
   * Constructs a `UPDATE` query with the given [[filter]] to select the
   * rows you want to delete
   */
  def update(filter: V[Column.ColumnExpr] => Expr[Boolean]): Update[V[Column.ColumnExpr], V[Id]] = {
    val ref = t.tableRef
    new Update.Impl(t.metadata.vExpr(ref), ref, Nil, Nil, Seq(filter(t.metadata.vExpr(ref))))(
      t.containerQr
    )
  }

  /**
   * Constructs a `INSERT` query
   */
  def insert: Insert[V[Column.ColumnExpr], V[Id]] = {
    val ref = t.tableRef
    new Insert.Impl(t.metadata.vExpr(ref), ref)(t.containerQr)
  }

  /**
   * Constructs a `DELETE` query with the given [[filter]] to select the
   * rows you want to delete
   */
  def delete(filter: V[Column.ColumnExpr] => Expr[Boolean]): Delete[V[Column.ColumnExpr]] = {
    val ref = t.tableRef
    new Delete.Impl(t.metadata.vExpr(ref), filter(t.metadata.vExpr(ref)), ref)
  }

  def isTrivialJoin = true
}
