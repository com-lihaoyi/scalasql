package scalasql.query

import scalasql.core.{DialectTypeMappers, Queryable, Expr, WithSqlExpr}

/**
 * A SQL `INSERT` query
 */
trait Insert[V[_[_]], R] extends WithSqlExpr[V[Column]] with scalasql.generated.Insert[V, R] {
  def table: TableRef
  def qr: Queryable[V[Column], R]
  def select[C, R2](columns: V[Expr] => C, select: Select[C, R2]): InsertSelect[V, C, R, R2]

  def columns(f: (V[Column] => Column.Assignment[?])*): InsertColumns[V, R]
  def values(f: R*): InsertValues[V, R]

  def batched[T1](f1: V[Column] => Column[T1])(items: Expr[T1]*): InsertColumns[V, R]

}

object Insert {
  class Impl[V[_[_]], R](val expr: V[Column], val table: TableRef)(
      implicit val qr: Queryable.Row[V[Column], R],
      dialect: DialectTypeMappers
  ) extends Insert[V, R]
      with scalasql.generated.InsertImpl[V, R] {

    def newInsertSelect[C, R, R2](
        insert: Insert[V, R],
        columns: C,
        select: Select[C, R2]
    ): InsertSelect[V, C, R, R2] = { new InsertSelect.Impl(insert, columns, select) }

    def newInsertValues[R](
        insert: Insert[V, R],
        columns: Seq[Column[?]],
        valuesLists: Seq[Seq[Expr[?]]]
    )(implicit qr: Queryable[V[Column], R]): InsertColumns[V, R] = {
      new InsertColumns.Impl(insert, columns, valuesLists)
    }

    def select[C, R2](columns: V[Expr] => C, select: Select[C, R2]): InsertSelect[V, C, R, R2] = {
      newInsertSelect(this, columns(expr.asInstanceOf[V[Expr]]), select)
    }

    def columns(f: (V[Column] => Column.Assignment[?])*): InsertColumns[V, R] = {
      val kvs = f.map(_(expr))
      newInsertValues(this, columns = kvs.map(_.column), valuesLists = Seq(kvs.map(_.value)))
    }

    def batched[T1](f1: V[Column] => Column[T1])(items: Expr[T1]*): InsertColumns[V, R] = {
      newInsertValues(this, columns = Seq(f1(expr)), valuesLists = items.map(Seq(_)))
    }

    override def values(values: R*): InsertValues[V, R] =
      new InsertValues.Impl(this, values, dialect, qr, Nil)
  }
}
