package scalasql.renderer

import SqlStr.SqlStringSyntax
import scalasql.query.{
  AscDesc,
  CompoundSelect,
  Expr,
  From,
  Join,
  Joinable,
  Nulls,
  Select,
  SimpleSelect,
  SubqueryRef,
  TableRef,
  WithCteRef
}
import scalasql.{Queryable, TypeMapper}
import scalasql.utils.FlatJson

object JoinsToSql {

  def joinsToSqlStr(
      joins: Seq[Join],
      renderedFroms: Map[From, SqlStr],
      joinOns: Seq[Seq[Option[SqlStr.Flattened]]]
  ) = {

    SqlStr.join(joins.zip(joinOns).map { case (join, joinOns) =>
      val joinPrefix = SqlStr.raw(join.prefix)
      val joinSelectables = SqlStr.join(join.from.zip(joinOns).map { case (jf, fromOns) =>
        val onSql = SqlStr.flatten(SqlStr.opt(fromOns)(on => sql" ON $on"))
        renderedFroms(jf.from) + onSql
      })

      sql" $joinPrefix $joinSelectables"
    })
  }

  def renderFroms(
      selectables: Seq[From],
      prevContext: Context,
      namedFromsMap: Map[From, String],
      liveExprs: Option[Set[Expr.Identity]]
  ) = {
    selectables.map { f => (f, renderSingleFrom(prevContext, liveExprs, f, namedFromsMap)) }.toMap
  }

  def renderSingleFrom(
      prevContext: Context,
      liveExprs: Option[Set[Expr.Identity]],
      f: From,
      namedFromsMap: Map[From, String]
  ): SqlStr = {
    val name = SqlStr.raw(namedFromsMap(f))
    f match {
      case t: TableRef =>
        SqlStr.raw(prevContext.config.tableNameMapper(t.value.tableName)) + sql" " + name

      case t: SubqueryRef[_, _] =>
        val toSqlQuery = Select.selectRenderer(t.value, prevContext)
        sql"(${toSqlQuery.render(liveExprs)}) $name"
      case t: WithCteRef[_, _] => name
    }
  }

  def renderLateralJoins(
      prevContext: Context,
      from: Seq[From],
      innerLiveExprs: Set[Expr.Identity],
      joins0: Seq[Join],
      renderedJoinOns: Seq[Seq[Option[SqlStr.Flattened]]]
  ) = {
    var joinContext = Context.compute(prevContext, from, None)

    val renderedFroms = JoinsToSql
      .renderFroms(from, prevContext, joinContext.fromNaming, Some(innerLiveExprs))
      .to(collection.mutable.Map)

    val joins = SqlStr.join(joins0.zip(renderedJoinOns).map { case (join, joinOns) =>
      val joinPrefix = SqlStr.raw(join.prefix)
      val prevJoinContext = joinContext
      joinContext = Context.compute(joinContext, join.from.map(_.from), None)
      val joinSelectables = SqlStr.join(join.from.zip(joinOns).map { case (jf, fromOns) =>
        val onSql = SqlStr.flatten(SqlStr.opt(fromOns)(on => sql" ON $on"))

        renderedFroms.getOrElseUpdate(
          jf.from,
          JoinsToSql.renderSingleFrom(
            prevJoinContext,
            Some(innerLiveExprs),
            jf.from,
            joinContext.fromNaming
          )
        ) +
          onSql
      })

      sql" $joinPrefix $joinSelectables"
    })
    (renderedFroms, joins)
  }
}
