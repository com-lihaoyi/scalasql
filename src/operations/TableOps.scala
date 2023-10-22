package usql.operations

import usql.{Column, Table}
import usql.query.{Expr, Insert, Joinable, Select, Update}

class TableOps[V[_[_]]](t: Table[V]) extends Joinable[V[Expr]] {
  def select: Select[V[Expr]] = {
    val ref = t.tableRef
    Select.fromTable(t.metadata.vExpr(ref).asInstanceOf[V[Expr]], ref)(t.containerQr)
  }

  def update: Update[V[Column.ColumnExpr]] = {
    val ref = t.tableRef
    Update.fromTable(t.metadata.vExpr(ref), ref)(t.containerQr)
  }

  def insert: Insert[V[Column.ColumnExpr]] = {
    val ref = t.tableRef
    Insert.fromTable(t.metadata.vExpr(ref), ref)(t.containerQr)
  }

  def isTrivialJoin = true
}
