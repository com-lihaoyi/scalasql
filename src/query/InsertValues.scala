package usql.query

import renderer.InsertToSql
import usql.renderer.{Context, SqlStr}
import usql.{Column, OptionPickler, Queryable}

case class Insert[Q](expr: Q, table: TableRef)
                    (implicit val qr: Queryable[Q, _]) {
  def values(f: (Q => (Column.ColumnExpr[_], Expr[_]))*): InsertValues[Q] = {
    val kvs = f.map(_(expr))
    InsertValues(this, columns = kvs.map(_._1), valuesLists = Seq(kvs.map(_._2)))
  }

  def batched[T1](f1: Q => Column.ColumnExpr[T1])
                 (items: Expr[T1]*): InsertValues[Q] = {
    InsertValues(
      this,
      columns = Seq(f1(expr)),
      valuesLists = items.map(Seq(_))
    )
  }

  def batched[T1, T2](f1: Q => Column.ColumnExpr[T1],
                      f2: Q => Column.ColumnExpr[T2])
                     (items: (Expr[T1], Expr[T2])*) = {
    InsertValues(
      this,
      columns = Seq(f1(expr), f2(expr)),
      valuesLists = items.map(t => Seq(t._1, t._2))
    )
  }

  def batched[T1, T2, T3](f1: Q => Column.ColumnExpr[T1],
                          f2: Q => Column.ColumnExpr[T2],
                          f3: Q => Column.ColumnExpr[T3])
                         (items: (Expr[T1], Expr[T2], Expr[T3])*): InsertValues[Q] = {
    InsertValues(
      this,
      columns = Seq(f1(expr), f2(expr), f3(expr)),
      valuesLists = items.map(t => Seq(t._1, t._2, t._3))
    )
  }

  def batched[T1, T2, T3, T4](f1: Q => Column.ColumnExpr[T1],
                              f2: Q => Column.ColumnExpr[T2],
                              f3: Q => Column.ColumnExpr[T3],
                              f4: Q => Column.ColumnExpr[T4])
                             (items: (Expr[T1], Expr[T2], Expr[T3], Expr[T4])*): InsertValues[Q] = {
    InsertValues(
      this,
      columns = Seq(f1(expr), f2(expr), f3(expr), f4(expr)),
      valuesLists = items.map(t => Seq(t._1, t._2, t._3, t._4))
    )
  }

  def batched[T1, T2, T3, T4, T5](f1: Q => Column.ColumnExpr[T1],
                                  f2: Q => Column.ColumnExpr[T2],
                                  f3: Q => Column.ColumnExpr[T3],
                                  f4: Q => Column.ColumnExpr[T4],
                                  f5: Q => Column.ColumnExpr[T5])
                                 (items: (Expr[T1], Expr[T2], Expr[T3], Expr[T4], Expr[T5])*): InsertValues[Q] = {
    InsertValues(
      this,
      columns = Seq(f1(expr), f2(expr), f3(expr), f4(expr), f5(expr)),
      valuesLists = items.map(t => Seq(t._1, t._2, t._3, t._4, t._5))
    )
  }

  def batched[T1, T2, T3, T4, T5, T6](f1: Q => Column.ColumnExpr[T1],
                                      f2: Q => Column.ColumnExpr[T2],
                                      f3: Q => Column.ColumnExpr[T3],
                                      f4: Q => Column.ColumnExpr[T4],
                                      f5: Q => Column.ColumnExpr[T5],
                                      f6: Q => Column.ColumnExpr[T6])
                                     (items: (Expr[T1], Expr[T2], Expr[T3], Expr[T4], Expr[T5], Expr[T6])*): InsertValues[Q] = {

    InsertValues(
      this,
      columns = Seq(f1(expr), f2(expr), f3(expr), f4(expr), f5(expr), f6(expr)),
      valuesLists = items.map(t => Seq(t._1, t._2, t._3, t._4, t._5, t._6))
    )
  }
}

object Insert{
  def fromTable[Q](expr: Q, table: TableRef)(implicit qr: Queryable[Q, _]): Insert[Q] = {
    Insert(expr, table)
  }
}

/**
 * Syntax reference
 *
 * https://www.postgresql.org/docs/current/sql-update.html
 */
case class InsertValues[Q](insert: Insert[Q],
                           columns: Seq[Column.ColumnExpr[_]],
                           valuesLists: Seq[Seq[Expr[_]]])
                          (implicit val qr: Queryable[Q, _]) {

  def returning[Q2, R](f: Q => Q2)(implicit qr: Queryable[Q2, R]): InsertReturning[Q2, R] = {
    InsertReturning(this, f(insert.expr))
  }
}

object InsertValues {


  implicit def InsertQueryable[Q](implicit qr: Queryable[Q, _]): Queryable[InsertValues[Q], Int] =
    new InsertQueryable[Q]()(qr)

  class InsertQueryable[Q](implicit qr: Queryable[Q, _]) extends Queryable[InsertValues[Q], Int] {
    override def isExecuteUpdate = true
    def walk(ur: InsertValues[Q]): Seq[(List[String], Expr[_])] = Nil

    override def singleRow = true

    def valueReader: OptionPickler.Reader[Int] = OptionPickler.IntReader

    override def toSqlQuery(q: InsertValues[Q], ctx0: Context): SqlStr = {
      InsertToSql(q, ctx0.tableNameMapper, ctx0.columnNameMapper)
    }
  }
}
