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

object SelectToSql {

  def joinsToSqlStr(
      joins: Seq[Join],
      fromSelectables: Map[From, (Map[Expr.Identity, SqlStr], SqlStr)]
  )(implicit ctx: Context) = {
    SqlStr.join(joins.map { join =>
      val joinPrefix = SqlStr.opt(join.prefix)(s => sql" ${SqlStr.raw(s)} ")
      val joinSelectables = SqlStr.join(join.from.map { jf =>
        fromSelectables(jf.from)._2 + SqlStr.opt(jf.on)(on => sql" ON $on")
      })

      sql"$joinPrefix JOIN $joinSelectables"
    })
  }

}
