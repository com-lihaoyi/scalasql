package scalasql.renderer

import scalasql.core.{Context, LiveExprs, SqlStr}
import scalasql.core.SqlStr.SqlStringSyntax
import scalasql.query.Join

object JoinsToSql {

  def joinsToSqlStr(
      joins: Seq[Join],
      renderedFroms: Map[Context.From, SqlStr],
      joinOns: Seq[Seq[Option[SqlStr.Flattened]]]
  ) = {

    SqlStr.join(joins.iterator.zip(joinOns).map { case (join, joinOns) =>
      val joinPrefix = SqlStr.raw(join.prefix)
      val joinSelectables = SqlStr.join(join.from.iterator.zip(joinOns).map { case (jf, fromOns) =>
        val onSql = SqlStr.flatten(SqlStr.opt(fromOns)(on => sql" ON $on"))
        renderedFroms(jf.from) + onSql
      })

      sql" $joinPrefix $joinSelectables"
    })
  }

  def renderFroms(
      selectables: Seq[Context.From],
      prevContext: Context,
      namedFromsMap: Map[Context.From, String],
      liveExprs: LiveExprs
  ) = {
    selectables.iterator.map { f =>
      (f, renderSingleFrom(prevContext, liveExprs, f, namedFromsMap))
    }.toMap
  }

  def renderSingleFrom(
      prevContext: Context,
      liveExprs: LiveExprs,
      f: Context.From,
      namedFromsMap: Map[Context.From, String]
  ): SqlStr = {
    f.renderSql(SqlStr.raw(namedFromsMap(f)), prevContext, liveExprs)
  }

  def renderLateralJoins(
      prevContext: Context,
      from: Seq[Context.From],
      innerLiveExprs: LiveExprs,
      joins0: Seq[Join],
      renderedJoinOns: Seq[Seq[Option[SqlStr.Flattened]]]
  ) = {
    var joinContext = Context.compute(prevContext, from, None)

    val renderedFroms = JoinsToSql
      .renderFroms(from, prevContext, joinContext.fromNaming, innerLiveExprs)
      .to(collection.mutable.Map)

    val joins = SqlStr.join(joins0.iterator.zip(renderedJoinOns).map { case (join, joinOns) =>
      val joinPrefix = SqlStr.raw(join.prefix)
      val prevJoinContext = joinContext
      joinContext = Context.compute(joinContext, join.from.map(_.from), None)
      val joinSelectables = SqlStr.join(join.from.iterator.zip(joinOns).map { case (jf, fromOns) =>
        val onSql = SqlStr.flatten(SqlStr.opt(fromOns)(on => sql" ON $on"))

        renderedFroms.getOrElseUpdate(
          jf.from,
          JoinsToSql.renderSingleFrom(
            prevJoinContext,
            innerLiveExprs,
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
