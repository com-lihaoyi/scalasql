package scalasql.dialects

import scalasql.renderer.SqlStr
import scalasql.renderer.SqlStr.SqlStringSyntax

object CompoundSelectRendererForceLimit {
  def limitToSqlStr(limit: Option[Int], offset: Option[Int]) = {
    SqlStr.opt(limit.orElse(Option.when(offset.nonEmpty)(Int.MaxValue))) { limit =>
      sql" LIMIT $limit"
    }
  }
}
