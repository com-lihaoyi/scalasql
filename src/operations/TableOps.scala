package usql.operations

import usql.{Column, Table, Val}
import usql.query.{Expr, Insert, Joinable, Select, Update}

class TableOps[V[_[_]]](t: Table[V]) extends Joinable[V[Expr], V[Val]] {
  def select: Select[V[Expr], V[Val]] = {
    val ref = t.tableRef
    Select.fromTable(t.metadata.vExpr(ref).asInstanceOf[V[Expr]], ref)(t.containerQr)
  }

  def update: Update[V[Column.ColumnExpr], V[Val]] = {
    val ref = t.tableRef
    Update.fromTable(t.metadata.vExpr(ref), ref)(t.containerQr)
  }

  def insert: Insert[V[Column.ColumnExpr], V[Val]] = {
    val ref = t.tableRef
    Insert.fromTable(t.metadata.vExpr(ref), ref)(t.containerQr)
  }

  def isTrivialJoin = true
}
