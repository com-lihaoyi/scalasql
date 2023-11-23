package scalasql.query

import scalasql.renderer.SqlStr.{Renderable, SqlStringSyntax}
import scalasql.{Queryable, TypeMapper}
import scalasql.renderer.{Context, SqlStr}
import scalasql.utils.OptionPickler

/**
 * A SQL `VALUES` clause, used to treat a sequence of primitive [[T]]s as
 * a [[Select]] query.
 */
class Values[T: TypeMapper](val ts: Seq[T])(implicit val qr: Queryable.Row[Expr[T], T])
    extends Select[Expr[T], T] {
  assert(ts.nonEmpty, "`Values` clause does not support empty sequence")
  def queryExpr[V: TypeMapper](f: Expr[T] => Context => SqlStr)(
      implicit qr: Queryable.Row[Expr[V], V]
  ): Expr[V] = selectSimpleFrom().queryExpr(f)

  protected def selectSimpleFrom() = this.subquery
  val tableRef = new SubqueryRef(this, qr)
  protected def columnName = "column1"
  protected val expr: Expr[T] = Expr { implicit ctx =>
    val prefix = ctx.fromNaming.get(tableRef) match {
      case Some("") => sql""
      case Some(s) => SqlStr.raw(s) + sql"."
      case None => sql"SCALASQL_MISSING_VALUES."
    }
    prefix + SqlStr.raw(ctx.config.columnNameMapper(columnName))
  }

  override protected def queryWalkExprs(): Seq[(List[String], Expr[_])] = Seq(Nil -> expr)

  protected def queryValueReader: OptionPickler.Reader[Seq[T]] =
    implicitly[OptionPickler.Reader[Seq[T]]]

  override protected def queryTypeMappers(): Seq[TypeMapper[_]] = Seq(implicitly[TypeMapper[T]])

  override def distinct: Select[Expr[T], T] = selectSimpleFrom().distinct

  override def map[Q2, R2](f: Expr[T] => Q2)(implicit qr: Queryable.Row[Q2, R2]) =
    selectSimpleFrom().map(f)

  override def flatMap[Q2, R2](f: Expr[T] => FlatJoin.Rhs[Q2, R2])(
      implicit qr: Queryable.Row[Q2, R2]
  ) =
    selectSimpleFrom().flatMap(f)

  def filter(f: Expr[T] => Expr[Boolean]): Select[Expr[T], T] = selectSimpleFrom().filter(f)

  def aggregate[E, V](f: SelectProxy[Expr[T]] => E)(implicit qr: Queryable.Row[E, V]) =
    selectSimpleFrom().aggregate(f)

  def mapAggregate[Q2, R2](
      f: (Expr[T], SelectProxy[Expr[T]]) => Q2
  )(implicit qr: Queryable.Row[Q2, R2]): Select[Q2, R2] = {
    selectSimpleFrom().mapAggregate(f)
  }

  def groupBy[K, V, R2, R3](groupKey: Expr[T] => K)(
      groupAggregate: SelectProxy[Expr[T]] => V
  )(implicit qrk: Queryable.Row[K, R2], qrv: Queryable.Row[V, R3]): Select[(K, V), (R2, R3)] =
    selectSimpleFrom().groupBy(groupKey)(groupAggregate)

  def sortBy(f: Expr[T] => Expr[_]): Select[Expr[T], T] = selectSimpleFrom().sortBy(f)

  override def asc: Select[Expr[T], T] = selectSimpleFrom().asc

  override def desc: Select[Expr[T], T] = selectSimpleFrom().desc

  override def nullsFirst: Select[Expr[T], T] = selectSimpleFrom().nullsFirst

  override def nullsLast: Select[Expr[T], T] = selectSimpleFrom().nullsLast

  override protected def compound0(
      op: String,
      other: Select[Expr[T], T]
  ): CompoundSelect[Expr[T], T] = ???

  override def drop(n: Int): Select[Expr[T], T] = selectSimpleFrom().drop(n)

  override def take(n: Int): Select[Expr[T], T] = selectSimpleFrom().take(n)

  override protected def selectRenderer(prevContext: Context): Select.Renderer =
    new Values.Renderer[T](this)(implicitly, prevContext)

  override def leftJoin[Q2, R2](other: Joinable[Q2, R2])(on: (Expr[T], Q2) => Expr[Boolean])(
      implicit joinQr: Queryable.Row[Q2, R2]
  ): Select[(Expr[T], JoinNullable[Q2]), (T, Option[R2])] =
    selectSimpleFrom().leftJoin(other)(on)

  override def rightJoin[Q2, R2](other: Joinable[Q2, R2])(on: (Expr[T], Q2) => Expr[Boolean])(
      implicit joinQr: Queryable.Row[Q2, R2]
  ): Select[(JoinNullable[Expr[T]], Q2), (Option[T], R2)] =
    selectSimpleFrom().rightJoin(other)(on)

  override def outerJoin[Q2, R2](other: Joinable[Q2, R2])(on: (Expr[T], Q2) => Expr[Boolean])(
      implicit joinQr: Queryable.Row[Q2, R2]
  ): Select[(JoinNullable[Expr[T]], JoinNullable[Q2]), (Option[T], Option[R2])] =
    selectSimpleFrom().outerJoin(other)(on)

  override protected def join0[Q2, R2](
      prefix: String,
      other: Joinable[Q2, R2],
      on: Option[(Expr[T], Q2) => Expr[Boolean]]
  )(implicit joinQr: Queryable.Row[Q2, R2]): Select[(Expr[T], Q2), (T, R2)] =
    selectSimpleFrom().join0(prefix, other, on)

  override protected def selectLhsMap(prevContext: Context): Map[Expr.Identity, SqlStr] = {
    Map(Expr.exprIdentity(expr) -> SqlStr.raw(columnName))
  }
}

object Values {
  class Renderer[T: TypeMapper](v: Values[T])(implicit ctx: Context) extends Select.Renderer {
    def wrapRow(t: T) = sql"($t)"
    def render(liveExprs: Option[Set[Expr.Identity]]): SqlStr = {
      val rows = SqlStr.join(v.ts.map(wrapRow), sql", ")
      sql"VALUES $rows"
    }

    def context = ctx
  }
}
