package scalasql.query

import scalasql.core.{DialectTypeMappers, Queryable, Expr, WithSqlExpr}

/**
 * A SQL `INSERT` query
 */
trait Insert[VExpr, VCol, R] extends WithSqlExpr[VCol] with scalasql.generated.Insert[VExpr, VCol, R] {
  def table: TableRef
  def qr: Queryable[VCol, R]
  def select[C, R2](columns: VExpr => C, select: Select[C, R2]): InsertSelect[VCol, C, R, R2]

  def columns(f: (VCol => Column.Assignment[?])*): InsertColumns[VCol, R]
  def values(f: R*): InsertValues[VCol, R]

  def batched[T1](f1: VCol => Column[T1])(items: Expr[T1]*): InsertColumns[VCol, R]

}

object Insert {
  class Impl[VExpr, VCol, R](val expr: VCol, val table: TableRef)(
      implicit val qr: Queryable.Row[VCol, R],
      dialect: DialectTypeMappers
  ) extends Insert[VExpr, VCol, R]
      with scalasql.generated.InsertImpl[VExpr, VCol, R] {

    def newInsertSelect[C, R, R2](
        insert: Insert[VExpr, VCol, R],
        columns: C,
        select: Select[C, R2]
    ): InsertSelect[VCol, C, R, R2] = { new InsertSelect.Impl(insert, columns, select) }

    def newInsertValues[R](
        insert: Insert[VExpr, VCol, R],
        columns: Seq[Column[?]],
        valuesLists: Seq[Seq[Expr[?]]]
    )(implicit qr: Queryable[VCol, R]): InsertColumns[VCol, R] = {
      new InsertColumns.Impl(insert, columns, valuesLists)
    }

    def select[C, R2](columns: VExpr => C, select: Select[C, R2]): InsertSelect[VCol, C, R, R2] = {
      newInsertSelect(this, columns(expr.asInstanceOf[VExpr]), select)
    }

    def columns(f: (VCol => Column.Assignment[?])*): InsertColumns[VCol, R] = {
      val kvs = f.map(_(expr))
      newInsertValues(this, columns = kvs.map(_.column), valuesLists = Seq(kvs.map(_.value)))
    }

    def batched[T1](f1: VCol => Column[T1])(items: Expr[T1]*): InsertColumns[VCol, R] = {
      newInsertValues(this, columns = Seq(f1(expr)), valuesLists = items.map(Seq(_)))
    }

    override def values(values: R*): InsertValues[VCol, R] =
      new InsertValues.Impl(this, values, dialect, qr, Nil)
  }
}
