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

  def walkLabels(q: Q): Seq[List[String]]
  def walkExprs(q: Q): Seq[Expr[_]]
  def walk(q: Q): Seq[(List[String], Expr[_])] = walkLabels(q).zip(walkExprs(q))
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
    def valueReader(q: Q): Reader[R] = valueReader()
    def valueReader(): Reader[R]
    def toTypeMappers(): Seq[TypeMapper[_]]
    def toTypeMappers(q: Q): Seq[TypeMapper[_]] = toTypeMappers()
    def walkLabels(): Seq[List[String]]
    def walkLabels(q: Q): Seq[List[String]] = walkLabels()
  }
  object Row extends scalasql.generated.QueryableRow {
    private[scalasql] class TupleNQueryable[Q, R](
        val walkLabels0: Seq[Seq[List[String]]],
        val walkExprs0: Q => Seq[Seq[Expr[_]]],
        val toTypeMappersList: Seq[Seq[TypeMapper[_]]],
        val valueReader0: Reader[R]
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
        ExprsToSql(walked, sql"", ctx)
      }

      def toTypeMappers(): Seq[TypeMapper[_]] = toTypeMappersList.flatten

      override def valueReader(): OptionPickler.Reader[R] = valueReader0
    }

    implicit def NullableQueryable[Q, R](
        implicit qr: Queryable.Row[Q, R]
    ): Queryable.Row[JoinNullable[Q], Option[R]] = new Queryable.Row[JoinNullable[Q], Option[R]] {
      def walkLabels() = qr.walkLabels()
      def walkExprs(q: JoinNullable[Q]) = qr.walkExprs(q.get)

      def valueReader(): OptionPickler.Reader[Option[R]] = {
        new OptionPickler.NullableReader(qr.valueReader())
          .asInstanceOf[OptionPickler.Reader[Option[R]]]
      }

      def toSqlStr(q: JoinNullable[Q], ctx: Context) = qr.toSqlStr(q.get, ctx)
      def toTypeMappers() = qr.toTypeMappers()
    }
  }

  implicit def QueryQueryable[R]: Queryable[Query[R], R] = new Query.Queryable[Query[R], R]()

}
