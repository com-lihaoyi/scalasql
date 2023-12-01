package scalasql

import renderer.{Context, ExprsToSql, JoinsToSql, SqlStr}
import scalasql.query.{Expr, JoinNullable, Query}
import scalasql.renderer.SqlStr.{Interp, SqlStringSyntax}

import java.sql.{PreparedStatement, ResultSet}

/**
 * Typeclass to indicate that we are able to evaluate a query of type [[Q]] to
 * return a result of type [[R]]. Involves two operations: flattening a structured
 * query to a flat list of expressions via [[walk]], and reading a JSON-ish
 * tree-shaped blob back into a return value via [[valueReader]]
 */
trait Queryable[-Q, R] {
  def isExecuteUpdate(q: Q): Boolean

  def walkLabels(q: Q): Seq[List[String]]
  def walkExprs(q: Q): Seq[Expr[_]]
  def walk(q: Q): Seq[(List[String], Expr[_])] = walkLabels(q).zip(walkExprs(q))
  def singleRow(q: Q): Boolean

  def toSqlStr(q: Q, ctx: Context): SqlStr
  def construct(q: Q, args: Queryable.ResultSetIterator): R

}

object Queryable {

  class ResultSetIterator(r: ResultSet) {
    var index = 0
    var nulls = 0
    var nonNulls = 0

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
    def isExecuteUpdate(q: Q): Boolean = false
    def singleRow(q: Q): Boolean = true
    def walkLabels(): Seq[List[String]]
    def walkLabels(q: Q): Seq[List[String]] = walkLabels()

    def construct(q: Q, args: ResultSetIterator): R = construct(args)
    def construct(args: ResultSetIterator): R
    def deconstruct(r: R): Q
  }
  object Row extends scalasql.generated.QueryableRow {
    private[scalasql] class TupleNQueryable[Q, R <: scala.Product](
        val walkLabels0: Seq[Seq[List[String]]],
        val walkExprs0: Q => Seq[Seq[Expr[_]]],
        construct0: ResultSetIterator => R,
        deconstruct0: R => Q
    ) extends Queryable.Row[Q, R] {
      def walkExprs(q: Q) = {
        walkExprs0(q).iterator.zipWithIndex
          .map { case (v, i) => (i.toString, v) }
          .flatMap { case (prefix, vs0) => vs0 }
          .toIndexedSeq
      }

      def walkLabels() = {
        walkLabels0.iterator.zipWithIndex
          .map { case (v, i) => (i.toString, v) }
          .flatMap { case (prefix, vs0) => vs0.map { k => prefix +: k } }
          .toIndexedSeq
      }

      def toSqlStr(q: Q, ctx: Context): SqlStr = {
        val walked = this.walk(q)
        ExprsToSql(walked, SqlStr.empty, ctx)
      }

      def construct(args: ResultSetIterator) = construct0(args)

      def deconstruct(r: R): Q = deconstruct0(r)
    }

    implicit def NullableQueryable[Q, R](
        implicit qr: Queryable.Row[Q, R]
    ): Queryable.Row[JoinNullable[Q], Option[R]] = new Queryable.Row[JoinNullable[Q], Option[R]] {
      def walkLabels() = qr.walkLabels()
      def walkExprs(q: JoinNullable[Q]) = qr.walkExprs(q.get)

      def toSqlStr(q: JoinNullable[Q], ctx: Context) = qr.toSqlStr(q.get, ctx)

      def construct(args: ResultSetIterator): Option[R] = {
        val startNonNulls = args.nonNulls
        val res = qr.construct(args)
        if (startNonNulls == args.nonNulls) None
        else Option(res)
      }

      def deconstruct(r: Option[R]): JoinNullable[Q] = JoinNullable(qr.deconstruct(r.get))
    }
  }

  implicit def QueryQueryable[R]: Queryable[Query[R], R] = new Query.Queryable[Query[R], R]()

}
