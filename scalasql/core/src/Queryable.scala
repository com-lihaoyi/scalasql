package scalasql.core

import scalasql.core.SqlStr.SqlStringSyntax
import scalasql.core.{Context, ExprsToSql}

import java.sql.ResultSet

/**
 * Typeclass to indicate that we are able to evaluate a query of type [[Q]] to
 * return a result of type [[R]]. Involves two operations: flattening a structured
 * query to a flat list of expressions via [[walkLabelsAndExprs]], and reading a JSON-ish
 * tree-shaped blob back into a return value via [[valueReader]]
 */
trait Queryable[-Q, R] {

  /**
   * Whether this queryable value is executed using `java.sql.Statement.getGeneratedKeys`
   * instead of `.executeQuery`.
   */
  def isGetGeneratedKeys(q: Q): Option[Queryable.Row[?, ?]]

  /**
   * Whether this queryable value is executed using `java.sql.Statement.executeUpdate`
   * instead of `.executeQuery`. Note that this needs to be known ahead of time, and
   * cannot be discovered by just calling `.execute`, because some JDBC drivers do not
   * properly handle updates in the `.execute` call
   */
  def isExecuteUpdate(q: Q): Boolean

  /**
   * Returns a sequence of labels, each represented by a list of tokens, representing
   * the expressions created by this queryable value. Used to add `AS foo_bar` labels
   * to the generated queries, to aid in readability
   */
  def walkLabels(q: Q): Seq[List[String]]

  /**
   * Returns a sequence of expressions created by this queryable value. Used to generate
   * the column list `SELECT` clauses, both for nested and top level `SELECT`s
   */
  def walkExprs(q: Q): Seq[Expr[?]]

  def walkLabelsAndExprs(q: Q): Queryable.Walked = walkLabels(q).zip(walkExprs(q))

  /**
   * Whether this query expects a single row to be returned, if so we can assert on
   * the number of rows and raise an error if 0 rows or 2+ rows are present
   */
  def isSingleRow(q: Q): Boolean

  /**
   * Converts the given queryable value into a [[SqlStr]], that can then be executed
   * by the underlying SQL JDBC interface
   */
  def renderSql(q: Q, ctx: Context): SqlStr

  /**
   * Construct a Scala return value from the [[Queryable.ResultSetIterator]] representing
   * the return value of this queryable value
   */
  def construct(q: Q, args: Queryable.ResultSetIterator): R
}

object Queryable {
  type Walked = Seq[(List[String], Expr[?])]
  class ResultSetIterator(r: ResultSet) {
    var index = 0
    var nulls = 0
    var nonNulls = 0

    def consumeNulls(columnsCount: Int): Boolean = {
      val result = Range.inclusive(index + 1, index + columnsCount).forall { i =>
        r.getObject(i) == null
      }
      if (result) index = index + columnsCount
      result
    }

    def get[T](mt: TypeMapper[T]) = {
      index += 1
      val res = mt.get(r, index)
      if (r.wasNull()) nulls += 1
      else nonNulls += 1
      res
    }
  }

  /**
   * A [[Queryable]] that represents a part of a single database row. [[Queryable.Row]]s
   * can be nested within other [[Queryable]]s, but [[Queryable]]s in general cannot. e.g.
   *
   * - `Select[Int]` is valid because `Select[Q]` takes a `Queryable.Row[Q]`, and
   *   there is a `Queryable.Row[Int]` available
   *
   * - `Select[Select[Int]]` is invalid because although there is a `Queryable[Select[Q]]`
   *   available, there is no `Queryable.Row[Select[Q]]`, as `Select[Q]` returns multiple rows
   */
  trait Row[Q, R] extends Queryable[Q, R] {
    def isGetGeneratedKeys(q: Q): Option[Queryable.Row[?, ?]] = None
    def isExecuteUpdate(q: Q): Boolean = false
    def isSingleRow(q: Q): Boolean = true
    def walkLabels(): Seq[List[String]]
    def walkLabels(q: Q): Seq[List[String]] = walkLabels()

    def renderSql(q: Q, ctx: Context): SqlStr = {
      ExprsToSql.apply(this.walkLabelsAndExprs(q), ctx, sql"SELECT ")
    }

    def construct(q: Q, args: ResultSetIterator): R = construct(args)
    def construct(args: ResultSetIterator): R

    /**
     * Takes the Scala-value of type [[R]] and converts it into a database-value of type [[Q]],
     * potentially representing multiple columns. Used for inserting Scala values into `INSERT`
     * or `VALUES` clauses
     */
    def deconstruct(r: R): Q

  }
  object Row extends scalasql.core.generated.QueryableRow {
    private[scalasql] class TupleNQueryable[Q, R <: scala.Product](
        val walkLabels0: Seq[Seq[List[String]]],
        val walkExprs0: Q => Seq[Seq[Expr[?]]],
        construct0: ResultSetIterator => R,
        deconstruct0: R => Q
    ) extends Queryable.Row[Q, R] {
      def walkExprs(q: Q): Seq[Expr[?]] = {
        walkExprs0(q).iterator.zipWithIndex
          .map { case (v, i) => (i.toString, v) }
          .flatMap { case (prefix, vs0) => vs0 }
          .toIndexedSeq
      }

      def walkLabels(): Seq[List[String]] = {
        walkLabels0.iterator.zipWithIndex
          .map { case (v, i) => (i.toString, v) }
          .flatMap { case (prefix, vs0) => vs0.map { k => prefix +: k } }
          .toIndexedSeq
      }

      def construct(args: ResultSetIterator) = construct0(args)

      def deconstruct(r: R): Q = deconstruct0(r)
    }

    implicit def NullableQueryable[Q, R](
        implicit qr: Queryable.Row[Q, R]
    ): Queryable.Row[JoinNullable[Q], Option[R]] = new Queryable.Row[JoinNullable[Q], Option[R]] {
      def walkLabels() = qr.walkLabels()

      def walkExprs(q: JoinNullable[Q]) = qr.walkExprs(q.get)

      def construct(args: Queryable.ResultSetIterator): Option[R] = {
        if (args.consumeNulls(qr.walkLabels().length)) {
          None
        } else {
          Option(qr.construct(args))
        }
      }

      def deconstruct(r: Option[R]): JoinNullable[Q] = JoinNullable(qr.deconstruct(r.get))
    }
  }
}
