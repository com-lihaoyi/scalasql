package scalasql.operations

import scalasql.Expr
import scalasql.query.{AscDesc, CompoundSelect, Nulls, OrderBy}
import scalasql.renderer.SqlStr.SqlStringSyntax
import scalasql.renderer.{Context, SqlStr}

case class WindowExpr[T](e: Expr[T],
                         partitionBy0: Option[Expr[_]],
                         orderBy: Seq[scalasql.query.OrderBy]) extends Expr[T] {
  protected def toSqlExpr0(implicit ctx: Context): SqlStr = {
    val partitionBySql = SqlStr.opt(partitionBy0) { p => sql"PARTITION BY $p" }
    val sortBySql = CompoundSelect.orderToSqlStr(orderBy, ctx)
    val overClause = SqlStr.join(
      Seq(partitionBySql, sortBySql).filter(!SqlStr.flatten(_).queryParts.forall(_.isEmpty)),
      sql" "
    )
    sql"$e OVER ($overClause)"

  }

  def partitionBy(e: Expr[_]) = this.copy(partitionBy0 = Some(e))

  def sortBy(expr: Expr[_]) = {
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
}
