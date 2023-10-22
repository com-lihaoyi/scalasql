package usql.renderer

import usql.query.{Expr, From, Select}

case class Context(
    fromNaming: Map[From, String],
    exprNaming: Map[Expr.Identity, SqlStr],
    tableNameMapper: String => String,
    columnNameMapper: String => String,
    defaultQueryableSuffix: String
)