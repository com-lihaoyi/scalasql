package scalasql.query
import scalasql.core.{Expr, SqlStr}
import scalasql.core.DialectTypeMappers
import scalasql.query.{AscDesc, CompoundSelect, Nulls, OrderBy}
import scalasql.core.SqlStr.SqlStringSyntax
import scalasql.core.Context

case class SqlWindow[T](
    e: Expr[T],
    partitionBy0: Option[Expr[?]],
    filter0: Option[Expr[Boolean]],
    orderBy: Seq[scalasql.query.OrderBy],
    frameStart0: Option[SqlStr],
    frameEnd0: Option[SqlStr],
    exclusions: Option[SqlStr]
)(implicit dialect: DialectTypeMappers)
    extends Expr[T] {
  import dialect.{dialectSelf => _, _}
  protected def renderToSql0(implicit ctx: Context): SqlStr = {
    val partitionBySql = SqlStr.opt(partitionBy0) { p => sql"PARTITION BY $p" }
    val sortBySql = CompoundSelect.orderToSqlStr(orderBy, ctx)
    val overClause = SqlStr.join(
      Seq(partitionBySql, sortBySql).filter(!SqlStr.flatten(_).queryParts.forall(_.length == 0)),
      sql" "
    )

    val frameStr = (frameStart0, frameEnd0, exclusions) match {
      case (None, None, None) => SqlStr.empty
      case (Some(start), None, ex) => sql" ROWS $start" + SqlStr.opt(ex)(sql" " + _)
      case (Some(start), Some(end), ex) =>
        sql" ROWS BETWEEN $start AND $end" + SqlStr.opt(ex)(sql" " + _)
    }
    val filterStr = SqlStr.opt(filter0) { f =>
      sql" FILTER (WHERE $f)"
    }
    sql"$e$filterStr OVER ($overClause$frameStr)"

  }

  def partitionBy(e: Expr[?]) = this.copy(partitionBy0 = Some(e))

  def filter(expr: Expr[Boolean]) = copy(filter0 = Some(expr))
  def sortBy(expr: Expr[?]) = {
    val newOrder = Seq(OrderBy(expr, None, None))

    copy(orderBy = newOrder ++ orderBy)
  }

  def asc =
    copy(orderBy = orderBy.take(1).map(_.copy(ascDesc = Some(AscDesc.Asc))) ++ orderBy.drop(1))

  def desc =
    copy(orderBy = orderBy.take(1).map(_.copy(ascDesc = Some(AscDesc.Desc))) ++ orderBy.drop(1))

  def nullsFirst =
    copy(orderBy = orderBy.take(1).map(_.copy(nulls = Some(Nulls.First))) ++ orderBy.drop(1))

  def nullsLast =
    copy(orderBy = orderBy.take(1).map(_.copy(nulls = Some(Nulls.Last))) ++ orderBy.drop(1))

  class FrameConfig(f: Some[SqlStr] => SqlWindow[T]) {
    def preceding(offset: Int = -1) = offset match {
      case -1 => f(Some(sql"UNBOUNDED PRECEDING"))
      case offset => f(Some(sql"$offset PRECEDING"))
    }

    def currentRow = f(Some(sql"CURRENT ROW"))

    def following(offset: Int = -1) = offset match {
      case -1 => f(Some(sql"UNBOUNDED FOLLOWING"))
      case offset => f(Some(sql"$offset FOLLOWING"))
    }
  }
  def frameStart = new FrameConfig(s => copy(frameStart0 = s))
  def frameEnd = new FrameConfig(s => copy(frameEnd0 = s))

  object exclude {
    def currentRow = copy(exclusions = Some(sql"EXCLUDE CURRENT ROW"))
    def group = copy(exclusions = Some(sql"EXCLUDE GROUP"))
    def ties = copy(exclusions = Some(sql"EXCLUDE TIES"))
    def noOthers = copy(exclusions = Some(sql"EXCLUDE NO OTHERS"))
  }
}
