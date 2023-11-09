package scalasql.query

import scalasql.renderer.{Context, SqlStr}
import scalasql.{MappedType, Queryable}

trait FlatMapJoinRhs[Q2, R2]
class FlatJoinMapResult[Q, Q2, R, R2](val from: From,
                                      val expr: Q,
                                      val on: Q => Expr[Boolean],
                                      val qr: Queryable.Row[Q2, R2],
                                      val f: Q => Q2) extends FlatMapJoinRhs[Q2, R2]

class FlatJoinFlatMapResult[Q, Q2, R, R2](val from: From,
                                          val expr: Q,
                                          val on: Q => Expr[Boolean],
                                          val qr: Queryable.Row[Q2, R2],
                                          val f: FlatMapJoinRhs[Q2, R2]) extends FlatMapJoinRhs[Q2, R2]

class FlatJoinMapper[Q, Q2, R, R2](from: From, expr: Q, on: Q => Expr[Boolean]) {
  def map(f: Q => Q2)
         (implicit qr: Queryable.Row[Q2, R2]): FlatJoinMapResult[Q, Q2, R, R2] = {
    new FlatJoinMapResult[Q, Q2, R, R2](from, expr, on, qr, f)
  }

  def flatMap(f: Q => FlatMapJoinRhs[Q2, R2])
             (implicit qr: Queryable.Row[Q2, R2]): FlatJoinFlatMapResult[Q, Q2, R, R2] = {
    new FlatJoinFlatMapResult[Q, Q2, R, R2](from, expr, on, qr, f(expr))
  }
}
/**
 * A SQL `SELECT` query, possible with `JOIN`, `WHERE`, `GROUP BY`,
 * `ORDER BY`, `LIMIT`, `OFFSET` clauses
 *
 * Models the various components of a SQL SELECT:
 *
 * {{{
 *  SELECT DISTINCT column, AGG_FUNC(column_or_expression), …
 *  FROM mytable
 *  JOIN another_table ON mytable.column = another_table.column
 *  WHERE constraint_expression
 *  GROUP BY column HAVING constraint_expression
 *  ORDER BY column ASC/DESC
 *  LIMIT count OFFSET COUNT;
 * }}}
 *
 * Good syntax reference:
 *
 * https://www.cockroachlabs.com/docs/stable/selection-queries#set-operations
 * https://www.postgresql.org/docs/current/sql-select.html
 */
trait Select[Q, R]
    extends SqlStr.Renderable
    with Aggregatable[Q]
    with From
    with Joinable[Q, R]
    with JoinOps[Select, Q, R]
    with Query.Multiple[R]
    with FlatMapJoinRhs[Q, R]{

  def toFromExpr = (new SubqueryRef(this, qr), expr)
  protected def newCompoundSelect[Q, R](
      lhs: SimpleSelect[Q, R],
      compoundOps: Seq[CompoundSelect.Op[Q, R]],
      orderBy: Seq[OrderBy],
      limit: Option[Int],
      offset: Option[Int]
  )(implicit qr: Queryable.Row[Q, R]): CompoundSelect[Q, R] =
    new CompoundSelect(lhs, compoundOps, orderBy, limit, offset)

  protected def newSimpleSelect[Q, R](
      expr: Q,
      exprPrefix: Option[String],
      from: Seq[From],
      joins: Seq[Join],
      where: Seq[Expr[_]],
      groupBy0: Option[GroupBy]
  )(implicit qr: Queryable.Row[Q, R]): SimpleSelect[Q, R] =
    new SimpleSelect(expr, exprPrefix, from, joins, where, groupBy0)

  def qr: Queryable.Row[Q, R]
  def isTrivialJoin: Boolean = false
  def select = this

  /**
   * Causes this [[Select]] to ignore duplicate rows, translates into SQL `SELECT DISTINCT`
   */
  def distinct: Select[Q, R]

  protected def subqueryRef(implicit qr: Queryable.Row[Q, R]) = new SubqueryRef[Q, R](this, qr)

  /**
   * Transforms the return value of this [[Select]] with the given function
   */
  def map[Q2, R2](f: Q => Q2)(implicit qr: Queryable.Row[Q2, R2]): Select[Q2, R2]

  /**
   * Performs an implicit `JOIN` between this [[Select]] and the one returned by the
   * callback function [[f]]
   */
  def flatMap[Q2, R2](f: Q => FlatMapJoinRhs[Q2, R2])
                     (implicit qr: Queryable.Row[Q2, R2]): Select[Q2, R2]

  /**
   * Filters this [[Select]] with the given predicate, translates into a SQL `WHERE` clause
   */
  def filter(f: Q => Expr[Boolean]): Select[Q, R]

  /**
   * Alias for [[filter]]
   */
  def withFilter(f: Q => Expr[Boolean]): Select[Q, R] = filter(f)

  /**
   * Performs one or more aggregates in a single [[Select]]
   */
  def aggregate[E, V](f: SelectProxy[Q] => E)(implicit qr: Queryable.Row[E, V]): Aggregate[E, V]

  /**
   * Translates into a SQL `GROUP BY`, takes a function specifying the group-key and
   * a function specifying the group-aggregate.
   */
  def groupBy[K, V, R2, R3](groupKey: Q => K)(
      groupAggregate: SelectProxy[Q] => V
  )(implicit qrk: Queryable.Row[K, R2], qrv: Queryable.Row[V, R3]): Select[(K, V), (R2, R3)]

  /**
   * Sorts this [[Select]] via the given expression. Translates into a SQL `ORDER BY`
   * clause. Can be called more than once to sort on multiple columns, with the last
   * call to [[sortBy]] taking priority. Can be followed by [[asc]], [[desc]], [[nullsFirst]]
   * or [[nullsLast]] to configure the sort order
   */
  def sortBy(f: Q => Expr[_]): Select[Q, R]

  /**
   * Combined with [[sortBy]] to make the sort order ascending, translates into SQL `ASC`
   */
  def asc: Select[Q, R]

  /**
   * Combined with [[sortBy]] to make the sort order descending, translates into SQL `DESC`
   */
  def desc: Select[Q, R]

  /**
   * Combined with [[sortBy]] to configure handling of nulls, translates into SQL `NULLS FIRST`
   */
  def nullsFirst: Select[Q, R]

  /**
   * Combined with [[sortBy]] to configure handling of nulls, translates into SQL `NULLS LAST`
   */
  def nullsLast: Select[Q, R]

  /**
   * Concatenates the result rows of this [[Select]] with another and removes duplicate
   * rows; translates into SQL `UNION`
   */
  def union(other: Select[Q, R]): Select[Q, R] = compound0("UNION", other)

  /**
   * Concatenates the result rows of this [[Select]] with another; translates into SQL
   * `UNION ALL`
   */
  def unionAll(other: Select[Q, R]): Select[Q, R] = compound0("UNION ALL", other)

  /**
   * Intersects the result rows of this [[Select]] with another, preserving only rows
   * present in both this and the [[other]] and removing duplicates. Translates
   * into SQL `INTERSECT`
   */
  def intersect(other: Select[Q, R]): Select[Q, R] = compound0("INTERSECT", other)

  /**
   * Subtracts the [[other]] from this [[Select]], returning only rows present this
   * but absent in [[other]], and removing duplicates. Translates into SQL `EXCEPT`
   */
  def except(other: Select[Q, R]): Select[Q, R] = compound0("EXCEPT", other)
  protected def compound0(op: String, other: Select[Q, R]): CompoundSelect[Q, R]

  /**
   * Drops the first [[n]] rows from this [[Select]]. Like when used in Scala collections,
   * if called multiple times the dropped rows add up
   */
  def drop(n: Int): Select[Q, R]

  /**
   * Only returns the first [[n]] rows from this [[Select]]. Like when used in Scala collections,
   * if called multiple times only the smallest value of [[n]] takes effect
   */
  def take(n: Int): Select[Q, R]

  def toSqlQuery(implicit ctx: Context): (SqlStr, Seq[MappedType[_]]) = {
    val renderer = getRenderer(ctx)

    (renderer.render(None).withCompleteQuery(true), renderer.mappedTypes)
  }
  def walk() = qr.walk(expr)
  override def singleRow = false

  def getRenderer(prevContext: Context): Select.Renderer

  /**
   * Asserts that this query returns exactly one row, and returns a single
   * value of type [[R]] rather than a `Seq[R]`. Throws an exception if
   * zero or multiple rows are returned.
   */
  def single: Query.Single[R] = new Query.Single(this)

  /**
   * Shorthand for `.take(1).single`:
   *
   * 1. If the query returns a single row, this returns it as a single value of type [[R]]
   * 2. If the query returns multiple rows, returns the first as a single value of type [[R]]
   *    and discards the rest
   * 3. If the query returns zero rows, throws an exception.
   */
  def head: Query.Single[R] = take(1).single

  /**
   * Converts this [[Select]] into an [[Expr]], assuming it returns a single row and
   * a single column. Note that if this returns multiple rows, behavior is database-specific,
   * with some like Sqlite simply taking the first row while others like Postgres/MySql
   * throwing exceptions
   */
  def toExpr(implicit mt: MappedType[R]): Expr[R] = Expr { implicit ctx => this.toSqlQuery._1 }

  protected def simpleFrom[Q, R](s: Select[Q, R]): SimpleSelect[Q, R] = s match {
    case s: SimpleSelect[Q, R] => s
    case s: CompoundSelect[Q, R] => s.subquery
  }

  /**
   * Forces this [[Select]] to be treated as a subquery that any further operations
   * will operate on, rather than having some operations flattened out into clauses
   * in this [[Select]]
   */
  def subquery: SimpleSelect[Q, R] = {
    newSimpleSelect(expr, None, Seq(subqueryRef(qr)), Nil, Nil, None)(qr)
  }




  /**
   * Performs a `LEFT JOIN` on the given [[other]], typically a [[Table]] or [[Select]].
   */
  def leftJoin[Q2, R2](other: Joinable[Q2, R2])(on: (Q, Q2) => Expr[Boolean])(
      implicit joinQr: Queryable.Row[Q2, R2]
  ): Select[(Q, Nullable[Q2]), (R, Option[R2])]

  /**
   * Performs a `RIGHT JOIN` on the given [[other]], typically a [[Table]] or [[Select]].
   */
  def rightJoin[Q2, R2](other: Joinable[Q2, R2])(on: (Q, Q2) => Expr[Boolean])(
      implicit joinQr: Queryable.Row[Q2, R2]
  ): Select[(Nullable[Q], Q2), (Option[R], R2)]

  /**
   * Performs a `OUTER JOIN` on the given [[other]], typically a [[Table]] or [[Select]].
   */
  def outerJoin[Q2, R2](other: Joinable[Q2, R2])(on: (Q, Q2) => Expr[Boolean])(
      implicit joinQr: Queryable.Row[Q2, R2]
  ): Select[(Nullable[Q], Nullable[Q2]), (Option[R], Option[R2])]

}

object Select {
  trait Renderer {
    def lhsMap: Map[Expr.Identity, SqlStr]
    def render(liveExprs: Option[Set[Expr.Identity]]): SqlStr
    def mappedTypes: Seq[MappedType[_]]
  }
}
