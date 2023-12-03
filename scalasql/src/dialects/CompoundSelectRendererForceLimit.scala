package scalasql.dialects

import scalasql.core.TypeMapper
import scalasql.core.SqlStr
import scalasql.core.SqlStr.SqlStringSyntax

object CompoundSelectRendererForceLimit {
  def limitToSqlStr(limit: Option[Int], offset: Option[Int])(implicit tm: TypeMapper[Int]) = {
    SqlStr.opt(limit.orElse(Option.when(offset.nonEmpty)(Int.MaxValue))) { limit =>
      sql" LIMIT $limit"
    }
  }
}
