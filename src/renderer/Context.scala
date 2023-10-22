package usql.renderer

import usql.query.{Expr, From, Select}

class Context(
    val fromNaming: Map[From, String],
    val exprNaming: Map[Expr.Identity, SqlStr],
    val tableNameMapper: String => String,
    val columnNameMapper: String => String,
) {
  def withAddedFromNaming(added: Map[From, String]) = new Context(
    added ++ fromNaming, exprNaming, tableNameMapper, columnNameMapper
  )
//  assert(!fromNaming.toString().contains("purchase"))
}
