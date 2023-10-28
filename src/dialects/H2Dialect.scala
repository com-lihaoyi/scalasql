package scalasql.dialects

import scalasql.{Column, MappedType, dialects, operations}
import scalasql.query.{Expr, InsertSelect, InsertValues, Query}
import scalasql.renderer.{Context, SqlStr}
import scalasql.renderer.SqlStr.SqlStringSyntax

object H2Dialect extends H2Dialect {
  class ExprStringOps(val v: Expr[String]) extends operations.ExprStringOps(v) with TrimOps
      with PadOps {
    def indexOf(x: Expr[String]): Expr[Int] = Expr { implicit ctx => sql"INSTR($v, $x)" }
  }

  class ExprNumericOps[T: Numeric: MappedType](val v: Expr[T])
      extends operations.ExprNumericOps[T](v) with BitwiseFunctionOps[T]

  class OnConflictable[Q, R](query: Query[R], expr: Q) extends scalasql.query.OnConflictable(query, expr) {

    override def onConflictIgnore(c: (Q => Column.ColumnExpr[_])*) =
      new OnConflictIgnore(this, c.map(_(expr)))
  }

  class OnConflictIgnore[Q, R](insert: OnConflictable[Q, R],
                               columns: Seq[Column.ColumnExpr[_]])
    extends scalasql.query.OnConflictIgnore[Q, R](insert, columns) {

    override def toSqlQuery(implicit ctx: Context): (SqlStr, Seq[MappedType[_]]) = {
      val (str, mapped) = insert.query.toSqlQuery
      val columnSqls = columns.map(c => SqlStr.raw(c.name) + sql" = " + SqlStr.raw(c.name))
      (
        str + sql" ON CONFLICT DO NOTHING",
        mapped
      )
    }

  }
}
trait H2Dialect extends Dialect {
  override implicit def ExprStringOpsConv(v: Expr[String]): H2Dialect.ExprStringOps =
    new H2Dialect.ExprStringOps(v)
  override implicit def ExprNumericOpsConv[T: Numeric: MappedType](v: Expr[T])
      : H2Dialect.ExprNumericOps[T] =
    new H2Dialect.ExprNumericOps(v)

  override implicit def OnConflictableInsertValues[Q, R](query: InsertValues[Q, R]) =
    new H2Dialect.OnConflictable[Q, Int](query, query.expr)

  override implicit def OnConflictableInsertSelect[Q, C, R, R2](query: InsertSelect[Q, C, R, R2]) =
    new H2Dialect.OnConflictable[Q, Int](query, query.expr)
}
