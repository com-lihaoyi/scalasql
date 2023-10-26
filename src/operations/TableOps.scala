package scalasql.operations

import scalasql.{Column, Id, Table}
import scalasql.query.{Delete, Expr, Insert, Joinable, Select, Update}

class TableOps[V[_[_]]](t: Table[V]) extends Joinable[V[Expr], V[Id]] {
  def select: Select[V[Expr], V[Id]] = {
    val ref = t.tableRef
    Select.fromTable(t.metadata.vExpr(ref).asInstanceOf[V[Expr]], ref)(t.containerQr)
  }

  def update: Update[V[Column.ColumnExpr], V[Id]] = {
    val ref = t.tableRef
    Update.fromTable(t.metadata.vExpr(ref), ref)(t.containerQr)
  }

  def insert: Insert[V[Column.ColumnExpr], V[Id]] = {
    val ref = t.tableRef
    Insert.fromTable(t.metadata.vExpr(ref), ref)(t.containerQr)
  }

  def delete(f: V[Column.ColumnExpr] => Expr[Boolean]): Delete[V[Column.ColumnExpr]] = {
    val ref = t.tableRef
    Delete.fromTable(t.metadata.vExpr(ref), f(t.metadata.vExpr(ref)), ref)
  }

  def isTrivialJoin = true
}
