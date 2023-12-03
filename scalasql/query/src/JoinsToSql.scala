package scalasql.renderer

import scalasql.core.{
  From,
  Queryable,
  Sql,
  SqlStr,
  Table,
  TypeMapper,
  SubqueryRef,
  TableRef,
  WithCteRef,
  FlatJson,
  Context
}
import scalasql.core.SqlStr.SqlStringSyntax
import scalasql.query.{AscDesc, CompoundSelect, Join, Joinable, Nulls, Select, SimpleSelect}

object JoinsToSql {

  def joinsToSqlStr(
      joins: Seq[Join],
      renderedFroms: Map[From, SqlStr],
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
      selectables: Seq[From],
      prevContext: Context,
      namedFromsMap: Map[From, String],
      liveExprs: Option[Set[Sql.Identity]]
  ) = {
    selectables.iterator.map { f =>
      (f, renderSingleFrom(prevContext, liveExprs, f, namedFromsMap))
    }.toMap
  }

  def renderSingleFrom(
      prevContext: Context,
      liveExprs: Option[Set[Sql.Identity]],
      f: From,
      namedFromsMap: Map[From, String]
  ): SqlStr = {
    val name = SqlStr.raw(namedFromsMap(f))
    f match {
      case t: TableRef =>
        SqlStr.raw(prevContext.config.tableNameMapper(Table.name(t.value))) + sql" " + name

      case t: SubqueryRef =>
        val renderSql = scalasql.core.SelectBase.renderer(t.value, prevContext)
        sql"(${renderSql.render(liveExprs)}) $name"

      case t: WithCteRef => name
    }
  }

  def renderLateralJoins(
      prevContext: Context,
      from: Seq[From],
      innerLiveExprs: Set[Sql.Identity],
      joins0: Seq[Join],
      renderedJoinOns: Seq[Seq[Option[SqlStr.Flattened]]]
  ) = {
    var joinContext = Context.compute(prevContext, from, None)

    val renderedFroms = JoinsToSql
      .renderFroms(from, prevContext, joinContext.fromNaming, Some(innerLiveExprs))
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