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
  SimpleSelect,
  SubqueryRef,
  TableRef
}
import scalasql.{MappedType, Queryable}
import scalasql.utils.FlatJson

object JoinsToSql {

  def joinsToSqlStr(
      joins: Seq[Join],
      fromSelectables: Map[
        From,
        (Map[Expr.Identity, SqlStr], Option[Set[Expr.Identity]] => SqlStr)
      ],
      liveExprs: Option[Set[Expr.Identity]],
      joinOns: Seq[Seq[Option[SqlStr.Flattened]]]
  )(implicit ctx: Context) = {

    SqlStr.join(joins.zip(joinOns).map { case (join, joinOns) =>
      val joinPrefix = SqlStr.opt(join.prefix)(s => sql" ${SqlStr.raw(s)}")
      val joinSelectables = SqlStr.join(join.from.zip(joinOns).map { case (jf, fromOns) =>
        val onSql = SqlStr.flatten(SqlStr.opt(fromOns)(on => sql" ON $on"))
        val onReferenced = onSql.referencedExprs
        val newLiveExprs = Some(liveExprs.getOrElse(Set.empty[Expr.Identity]) ++ onReferenced)
        fromSelectables(jf.from)._2(newLiveExprs) + onSql
      })

      sql"$joinPrefix JOIN $joinSelectables"
    })
  }
}
