package scalasql

import utils.OptionPickler.Reader
import renderer.{Context, ExprsToSql, JoinsToSql, SqlStr}
import scalasql.query.{Expr, JoinNullable, Query}
import scalasql.renderer.SqlStr.SqlStringSyntax
import scalasql.utils.OptionPickler

/**
 * Typeclass to indicate that we are able to evaluate a query of type [[Q]] to
 * return a result of type [[R]]. Involves two operations: flattening a structured
 * query to a flat list of expressions via [[walk]], and reading a JSON-ish
 * tree-shaped blob back into a return value via [[valueReader]]
 */
trait Queryable[-Q, R] {
  def isExecuteUpdate(q: Q): Boolean
  def walk(q: Q): Seq[(List[String], Expr[_])]
  def valueReader(q: Q): Reader[R]
  def singleRow(q: Q): Boolean

  def toSqlStr(q: Q, ctx: Context): SqlStr
  def toTypeMappers(q: Q): Seq[TypeMapper[_]]
}

object Queryable {

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
  trait Row[-Q, R] extends Queryable[Q, R] {
    def isExecuteUpdate(q: Q): Boolean = false
    def singleRow(q: Q): Boolean = true
    def toTypeMappers0: Seq[TypeMapper[_]]
    def toTypeMappers(q: Q): Seq[TypeMapper[_]] = toTypeMappers0
  }
  object Row extends scalasql.generated.QueryableRow {
    private[scalasql] class TupleNQueryable[Q, R](
        val walk0: Q => Seq[Seq[(List[String], Expr[_])]],
        val toTypeMappers0List: Seq[Seq[TypeMapper[_]]],
        val valueReader0: Q => Reader[R]
    ) extends Queryable.Row[Q, R] {
      def walk(q: Q) = {
        walk0(q).iterator.zipWithIndex
          .map { case (v, i) => (i.toString, v) }
          .flatMap { case (prefix, vs0) => vs0.map { case (k, v) => (prefix +: k, v) } }
          .toIndexedSeq
      }

      def toSqlStr(q: Q, ctx: Context): SqlStr = {
        val walked = this.walk(q)
        ExprsToSql(walked, sql"", ctx)
      }

      def toTypeMappers0: Seq[TypeMapper[_]] = toTypeMappers0List.flatten

      override def valueReader(q: Q): OptionPickler.Reader[R] = valueReader0(q)
    }

    implicit def NullableQueryable[Q, R](
        implicit qr: Queryable.Row[Q, R]
    ): Queryable.Row[JoinNullable[Q], Option[R]] = new Queryable.Row[JoinNullable[Q], Option[R]] {
      def walk(q: JoinNullable[Q]): Seq[(List[String], Expr[_])] = qr.walk(q.get)

      def valueReader(q: JoinNullable[Q]): OptionPickler.Reader[Option[R]] = {
        new OptionPickler.NullableReader(qr.valueReader(q.get))
          .asInstanceOf[OptionPickler.Reader[Option[R]]]
      }

      def toSqlStr(q: JoinNullable[Q], ctx: Context) = qr.toSqlStr(q.get, ctx)
      def toTypeMappers0 = qr.toTypeMappers0
    }
  }

  implicit def QueryQueryable[R]: Queryable[Query[R], R] = new Query.Queryable[Query[R], R]()

}
