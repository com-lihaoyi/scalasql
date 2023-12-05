package scalasql.query

import scalasql.core.{
  Aggregatable,
  Context,
  DialectTypeMappers,
  JoinNullable,
  LiveSqlExprs,
  Queryable,
  Sql,
  SqlStr,
  TypeMapper
}
import scalasql.core.SqlStr.SqlStringSyntax

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
    with Joinable[Q, R]
    with JoinOps[Select, Q, R]
    with Query.Multiple[R]
    with SelectBase {

  protected def dialect: DialectTypeMappers
  protected def joinableToFromExpr = (new SubqueryRef(this, qr), expr)
  protected def newCompoundSelect[Q, R](
      lhs: SimpleSelect[Q, R],
      compoundOps: Seq[CompoundSelect.Op[Q, R]],
      orderBy: Seq[OrderBy],
      limit: Option[Int],
      offset: Option[Int]
  )(implicit qr: Queryable.Row[Q, R], dialect: DialectTypeMappers): CompoundSelect[Q, R] =
    new CompoundSelect(lhs, compoundOps, orderBy, limit, offset)

  protected def newSimpleSelect[Q, R](
      expr: Q,
      exprPrefix: Option[Context => SqlStr],
      from: Seq[Context.From],
      joins: Seq[Join],
      where: Seq[Sql[_]],
      groupBy0: Option[GroupBy]
  )(implicit qr: Queryable.Row[Q, R], dialect: DialectTypeMappers): SimpleSelect[Q, R] =
    new SimpleSelect(expr, exprPrefix, from, joins, where, groupBy0)

  def qr: Queryable.Row[Q, R]
  protected def joinableIsTrivial: Boolean = false
  protected def joinableToSelect = this

  /**
   * Causes this [[Select]] to ignore duplicate rows, translates into SQL `SELECT DISTINCT`
   */
  def distinct: Select[Q, R] = selectWithExprPrefix(ctx => sql"DISTINCT")
  protected def selectWithExprPrefix(s: Context => SqlStr): Select[Q, R]

  protected def subqueryRef(implicit qr: Queryable.Row[Q, R]) = new SubqueryRef(this, qr)

  /**
   * Transforms the return value of this [[Select]] with the given function
   */
  def map[Q2, R2](f: Q => Q2)(implicit qr: Queryable.Row[Q2, R2]): Select[Q2, R2]

  /**
   * Performs an implicit `JOIN` between this [[Select]] and the one returned by the
   * callback function [[f]]
   */
  def flatMap[Q2, R2](f: Q => FlatJoin.Rhs[Q2, R2])(
      implicit qr: Queryable.Row[Q2, R2]
  ): Select[Q2, R2]

  /**
   * Filters this [[Select]] with the given predicate, translates into a SQL `WHERE` clause
   */
  def filter(f: Q => Sql[Boolean]): Select[Q, R]

  /**
   * Alias for [[filter]]
   */
  def withFilter(f: Q => Sql[Boolean]): Select[Q, R] = filter(f)

  /**
   * Performs one or more aggregates in a single [[Select]]
   */
  def aggregate[E, V](f: SelectProxy[Q] => E)(implicit qr: Queryable.Row[E, V]): Aggregate[E, V]

  /**
   * Performs a `.map` which additionally provides a [[SelectProxy]] that allows you to perform aggregate
   * functions.
   */
  def mapAggregate[Q2, R2](f: (Q, SelectProxy[Q]) => Q2)(
      implicit qr: Queryable.Row[Q2, R2]
  ): Select[Q2, R2]

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
  def sortBy(f: Q => Sql[_]): Select[Q, R]

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

  protected def renderToSql(ctx: Context): SqlStr = {
    val renderer = selectRenderer(ctx)

    renderer.render(LiveSqlExprs.none).withCompleteQuery(true)
  }
  protected def queryWalkLabels() = qr.walkLabels(expr)
  protected def queryWalkExprs() = qr.walkExprs(expr)
  protected override def queryIsSingleRow = false

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
   * Converts this [[Select]] into an [[Sql]], assuming it returns a single row and
   * a single column. Note that if this returns multiple rows, behavior is database-specific,
   * with some like Sqlite simply taking the first row while others like Postgres/MySql
   * throwing exceptions
   */
  def toExpr(implicit mt: TypeMapper[R]): Sql[R] = Sql { implicit ctx => this.renderToSql(ctx) }

  protected def selectToSimpleSelect(): SimpleSelect[Q, R]

  /**
   * Forces this [[Select]] to be treated as a subquery that any further operations
   * will operate on, rather than having some operations flattened out into clauses
   * in this [[Select]]
   */
  def subquery: SimpleSelect[Q, R] = {
    newSimpleSelect(expr, None, Seq(subqueryRef(qr)), Nil, Nil, None)(qr, dialect)
  }

  /**
   * Performs a `LEFT JOIN` on the given [[other]], typically a [[Table]] or [[Select]].
   */
  def leftJoin[Q2, R2](other: Joinable[Q2, R2])(on: (Q, Q2) => Sql[Boolean])(
      implicit joinQr: Queryable.Row[Q2, R2]
  ): Select[(Q, JoinNullable[Q2]), (R, Option[R2])]

  /**
   * Performs a `RIGHT JOIN` on the given [[other]], typically a [[Table]] or [[Select]].
   */
  def rightJoin[Q2, R2](other: Joinable[Q2, R2])(on: (Q, Q2) => Sql[Boolean])(
      implicit joinQr: Queryable.Row[Q2, R2]
  ): Select[(JoinNullable[Q], Q2), (Option[R], R2)]

  /**
   * Performs a `OUTER JOIN` on the given [[other]], typically a [[Table]] or [[Select]].
   */
  def outerJoin[Q2, R2](other: Joinable[Q2, R2])(on: (Q, Q2) => Sql[Boolean])(
      implicit joinQr: Queryable.Row[Q2, R2]
  ): Select[(JoinNullable[Q], JoinNullable[Q2]), (Option[R], Option[R2])]

  /**
   * Returns whether or not the [[Select]] on the left contains the [[other]] value on the right
   */
  def contains(other: Q): Sql[Boolean] = Sql { implicit ctx =>
    val lhs = qr.walkExprs(other).map(e => sql"$e") match {
      case Seq(single) => single
      case multiple => sql"(${SqlStr.join(multiple, SqlStr.commaSep)})"
    }
    sql"($lhs IN $this)"
  }

  /**
   * Returns whether or not the [[Select]] on the left is empty with zero elements
   */
  def isEmpty: Sql[Boolean] = Sql { implicit ctx => sql"(NOT EXISTS $this)" }

  /**
   * Returns whether or not the [[Select]] on the left is nonempty with one or more elements
   */
  def nonEmpty: Sql[Boolean] = Sql { implicit ctx => sql"(EXISTS $this)" }
}

object Select {
  def toSimpleFrom[Q, R](s: Select[Q, R]) = s.selectToSimpleSelect()

  def withExprPrefix[Q, R](s: Select[Q, R], str: Context => SqlStr) =
    s.selectWithExprPrefix(str)

  implicit class ExprSelectOps[T](s: Select[Sql[T], T]) {
    def sorted(implicit tm: TypeMapper[T]): Select[Sql[T], T] = s.sortBy(identity)
  }

  trait Proxy[Q, R] extends Select[Q, R] {
    override def qr: Queryable.Row[Q, R]
    override protected def selectWithExprPrefix(s: Context => SqlStr): Select[Q, R] =
      selectToSimpleSelect().selectWithExprPrefix(s)

    override def map[Q2, R2](f: Q => Q2)(implicit qr: Queryable.Row[Q2, R2]): Select[Q2, R2] =
      selectToSimpleSelect().map(f)

    override def flatMap[Q2, R2](f: Q => FlatJoin.Rhs[Q2, R2])(
        implicit qr: Queryable.Row[Q2, R2]
    ): Select[Q2, R2] = selectToSimpleSelect().flatMap(f)

    override def filter(f: Q => Sql[Boolean]): Select[Q, R] = selectToSimpleSelect().filter(f)

    override def aggregate[E, V](f: SelectProxy[Q] => E)(
        implicit qr: Queryable.Row[E, V]
    ): Aggregate[E, V] = selectToSimpleSelect().aggregate(f)

    override def mapAggregate[Q2, R2](f: (Q, SelectProxy[Q]) => Q2)(
        implicit qr: Queryable.Row[Q2, R2]
    ): Select[Q2, R2] = selectToSimpleSelect().mapAggregate(f)

    override def groupBy[K, V, R2, R3](groupKey: Q => K)(
        groupAggregate: SelectProxy[Q] => V
    )(implicit qrk: Queryable.Row[K, R2], qrv: Queryable.Row[V, R3]): Select[(K, V), (R2, R3)] =
      selectToSimpleSelect().groupBy(groupKey)(groupAggregate)

    override def sortBy(f: Q => Sql[_]): Select[Q, R] = selectToSimpleSelect().sortBy(f)
    override def asc: Select[Q, R] = selectToSimpleSelect().asc
    override def desc: Select[Q, R] = selectToSimpleSelect().desc
    override def nullsFirst: Select[Q, R] = selectToSimpleSelect().nullsFirst
    override def nullsLast: Select[Q, R] = selectToSimpleSelect().nullsLast

    override protected def compound0(op: String, other: Select[Q, R]): CompoundSelect[Q, R] =
      selectToSimpleSelect().compound0(op, other)

    override def drop(n: Int): Select[Q, R] = selectToSimpleSelect().drop(n)
    override def take(n: Int): Select[Q, R] = selectToSimpleSelect().take(n)

    override protected def selectRenderer(prevContext: Context): SelectBase.Renderer =
      SelectBase.renderer(selectToSimpleSelect(), prevContext)

    override protected def selectLhsMap(prevContext: Context): Map[Sql.Identity, SqlStr] =
      SelectBase.lhsMap(selectToSimpleSelect(), prevContext)

    override protected def selectToSimpleSelect(): SimpleSelect[Q, R]

    override def leftJoin[Q2, R2](other: Joinable[Q2, R2])(on: (Q, Q2) => Sql[Boolean])(
        implicit joinQr: Queryable.Row[Q2, R2]
    ): Select[(Q, JoinNullable[Q2]), (R, Option[R2])] = selectToSimpleSelect().leftJoin(other)(on)

    override def rightJoin[Q2, R2](other: Joinable[Q2, R2])(on: (Q, Q2) => Sql[Boolean])(
        implicit joinQr: Queryable.Row[Q2, R2]
    ): Select[(JoinNullable[Q], Q2), (Option[R], R2)] = selectToSimpleSelect().rightJoin(other)(on)

    override def outerJoin[Q2, R2](other: Joinable[Q2, R2])(on: (Q, Q2) => Sql[Boolean])(
        implicit joinQr: Queryable.Row[Q2, R2]
    ): Select[(JoinNullable[Q], JoinNullable[Q2]), (Option[R], Option[R2])] =
      selectToSimpleSelect().outerJoin(other)(on)

    override protected def queryConstruct(args: Queryable.ResultSetIterator): Seq[R] =
      Query.construct(selectToSimpleSelect(), args)

    override protected def join0[Q2, R2, QF, RF](
        prefix: String,
        other: Joinable[Q2, R2],
        on: Option[(Q, Q2) => Sql[Boolean]]
    )(implicit ja: JoinAppend[Q, Q2, QF, RF]): Select[QF, RF] =
      selectToSimpleSelect().join0(prefix, other, on)

    override def queryExpr[V: TypeMapper](f: Q => Context => SqlStr)(
        implicit qr: Queryable.Row[Sql[V], V]
    ): Sql[V] = selectToSimpleSelect().queryExpr(f)

    override protected def expr: Q = selectToSimpleSelect().expr
  }
}
