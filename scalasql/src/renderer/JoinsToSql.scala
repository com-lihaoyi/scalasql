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
  TableRef
}
import scalasql.{Queryable, TypeMapper}
import scalasql.utils.FlatJson

object JoinsToSql {

  def joinsToSqlStr(
      joins: Seq[Join],
      renderedFroms: Map[
        From,
        SqlStr
      ],
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
  ) = selectables
    .map { f =>
      val name = SqlStr.raw(namedFromsMap(f))
      (
        f,
        f match {
          case t: TableRef =>
            SqlStr.raw(prevContext.config.tableNameMapper(t.value.tableName)) + sql" " + name

          case t: SubqueryRef[_, _] =>
            val toSqlQuery = Select.getRenderer(t.value, prevContext)
            sql"(${toSqlQuery.render(liveExprs)}) $name"
        }
      )
    }
    .toMap

}
