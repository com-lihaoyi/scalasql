package scalasql.dialects

import scalasql.renderer.SqlStr
import scalasql.renderer.SqlStr.SqlStringSyntax

object CompoundSelectRendererForceLimit {
  def limitOffsetToSqlStr(limit: Option[Int], offset: Option[Int]) = {
    val limitOpt = SqlStr.opt(limit.orElse(Option.when(offset.nonEmpty)(Int.MaxValue))) { limit =>
      sql" LIMIT " + SqlStr.raw(limit.toString)
    }

    val offsetOpt = SqlStr.opt(offset) { offset => sql" OFFSET " + SqlStr.raw(offset.toString) }
    (limitOpt, offsetOpt)
  }
}
