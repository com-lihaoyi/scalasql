package scalasql.core

trait DialectConfig {
  protected def dialectCastParams: Boolean

  def renderSql[Q, R](query: Q, config: Config, castParams: Boolean = false)(
      implicit qr: Queryable[Q, R]
  ): String = {
    val flattened = DialectConfig.unpackQueryable(query, qr, config)
    DialectConfig.combineQueryString(flattened, castParams)
  }
}

object DialectConfig {
  def castParams(d: DialectConfig) = d.dialectCastParams

  def unpackQueryable[R, Q](query: Q, qr: Queryable[Q, R], config: Config) = {
    val ctx = Context.Impl(Map(), Map(), config)
    val flattened = SqlStr.flatten(qr.toSqlStr(query, ctx))
    flattened
  }

  def combineQueryString(flattened: SqlStr.Flattened, castParams: Boolean) = {
    val queryStr = flattened.queryParts.iterator
      .zipAll(flattened.params, "", null)
      .map {
        case (part, null) => part
        case (part, param) =>
          val jdbcTypeString = param.mappedType.castTypeString
          if (castParams) part + s"CAST(? AS $jdbcTypeString)" else part + "?"
      }
      .mkString

    queryStr
  }

}
