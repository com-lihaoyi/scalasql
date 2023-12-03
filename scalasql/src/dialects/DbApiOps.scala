package scalasql.dialects

import scalasql.core._
import scalasql.query.{Select, Values, WithCte, WithCteRef}

class DbApiOps(dialect: DialectBase) {
  import dialect._

  /**
   * Creates a SQL `CASE`/`WHEN`/`ELSE` clause
   */
  def caseWhen[T: TypeMapper](values: (Sql[Boolean], Sql[T])*) =
    new scalasql.operations.CaseWhen(values)

  /**
   * Creates a SQL `VALUES` clause
   */
  def values[Q, R](ts: Seq[R])(implicit qr: Queryable.Row[Q, R]): Values[Q, R] =
    new scalasql.query.Values(ts)

  import scalasql.core.SqlStr.SqlStringSyntax

  /**
   * The row_number() of the first peer in each group - the rank of the current row
   * with gaps. If there is no ORDER BY clause, then all rows are considered peers and
   * this function always returns 1.
   */
  def rank(): Sql[Int] = Sql { implicit ctx => sql"RANK()" }

  /**
   * The number of the row within the current partition. Rows are numbered starting
   * from 1 in the order defined by the ORDER BY clause in the window definition, or
   * in arbitrary order otherwise.
   */
  def rowNumber(): Sql[Int] = Sql { implicit ctx => sql"ROW_NUMBER()" }

  /**
   * The number of the current row's peer group within its partition - the rank of the
   * current row without gaps. Rows are numbered starting from 1 in the order defined
   * by the ORDER BY clause in the window definition. If there is no ORDER BY clause,
   * then all rows are considered peers and this function always returns 1.
   */
  def denseRank(): Sql[Int] = Sql { implicit ctx => sql"DENSE_RANK()" }

  /**
   * Despite the name, this function always returns a value between 0.0 and 1.0 equal to
   * (rank - 1)/(partition-rows - 1), where rank is the value returned by built-in window
   * function rank() and partition-rows is the total number of rows in the partition. If
   * the partition contains only one row, this function returns 0.0.
   */
  def percentRank(): Sql[Double] = Sql { implicit ctx => sql"PERCENT_RANK()" }

  /**
   * The cumulative distribution. Calculated as row-number/partition-rows, where row-number
   * is the value returned by row_number() for the last peer in the group and partition-rows
   * the number of rows in the partition.
   */
  def cumeDist(): Sql[Double] = Sql { implicit ctx => sql"CUME_DIST()" }

  /**
   * Argument N is handled as an integer. This function divides the partition into N groups
   * as evenly as possible and assigns an integer between 1 and N to each group, in the order
   * defined by the ORDER BY clause, or in arbitrary order otherwise. If necessary, larger
   * groups occur first. This function returns the integer value assigned to the group that
   * the current row is a part of.
   */
  def ntile(n: Int): Sql[Int] = Sql { implicit ctx => sql"NTILE($n)" }

  private def lagLead[T](prefix: SqlStr, e: Sql[T], offset: Int, default: Sql[T]): Sql[T] =
    Sql { implicit ctx =>
      val args = SqlStr.join(
        Seq(
          Some(sql"$e"),
          Some(offset).filter(_ != -1).map(o => sql"$o"),
          Option(default).map(d => sql"$d")
        ).flatten,
        SqlStr.commaSep
      )

      sql"$prefix($args)"
    }

  /**
   * The lag() function returns the result of evaluating expression expr against the
   * previous row in the partition. Or, if there is no previous row (because the current
   * row is the first), NULL.
   *
   * If the offset argument is provided, then it must be a non-negative integer. In this
   * case the value returned is the result of evaluating expr against the row offset rows
   * before the current row within the partition. If offset is 0, then expr is evaluated
   * against the current row. If there is no row offset rows before the current row, NULL
   * is returned.
   *
   * If default is also provided, then it is returned instead of NULL if the row identified
   * by offset does not exist.
   */
  def lag[T](e: Sql[T], offset: Int = -1, default: Sql[T] = null): Sql[T] =
    lagLead(sql"LAG", e, offset, default)

  /**
   * The lead() function returns the result of evaluating expression expr against the next
   * row in the partition. Or, if there is no next row (because the current row is the last),
   * NULL.
   *
   * If the offset argument is provided, then it must be a non-negative integer. In this
   * case the value returned is the result of evaluating expr against the row offset rows
   * after the current row within the partition. If offset is 0, then expr is evaluated
   * against the current row. If there is no row offset rows after the current row, NULL
   * is returned.
   *
   * If default is also provided, then it is returned instead of NULL if the row identified
   * by offset does not exist.
   */
  def lead[T](e: Sql[T], offset: Int = -1, default: Sql[T] = null): Sql[T] =
    lagLead(sql"LEAD", e, offset, default)

  /**
   * Calculates the window frame for each row in the same way as an aggregate window
   * function. It returns the value of expr evaluated against the first row in the window
   * frame for each row.
   */
  def firstValue[T](e: Sql[T]): Sql[T] = Sql { implicit ctx => sql"FIRST_VALUE($e)" }

  /**
   * Calculates the window frame for each row in the same way as an aggregate window
   * function. It returns the value of expr evaluated against the last row in the window
   * frame for each row.
   */
  def lastValue[T](e: Sql[T]): Sql[T] = Sql { implicit ctx => sql"LAST_VALUE($e)" }

  /**
   * Calculates the window frame for each row in the same way as an aggregate window
   * function. It returns the value of expr evaluated against the row N of the window
   * frame. Rows are numbered within the window frame starting from 1 in the order
   * defined by the ORDER BY clause if one is present, or in arbitrary order otherwise.
   * If there is no Nth row in the partition, then NULL is returned.
   */
  def nthValue[T](e: Sql[T], n: Int): Sql[T] = Sql { implicit ctx => sql"NTH_VALUE($e, $n)" }

  /** Generates a SQL `WITH` common table expression clause */
  def withCte[Q, Q2, R, R2](
      lhs: Select[Q, R]
  )(block: Select[Q, R] => Select[Q2, R2])(implicit qr: Queryable.Row[Q2, R2]): Select[Q2, R2] = {
    val lhsSubQueryRef = new WithCteRef()
    val rhsSelect = new WithCte.Proxy[Q, R](lhs, lhsSubQueryRef, lhs.qr, dialect)

    new WithCte(lhs, lhsSubQueryRef, block(rhsSelect))
  }
}
