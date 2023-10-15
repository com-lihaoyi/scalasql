package usql.renderer

import usql.query.{Expr, Select}

class Context(val fromNaming: Map[Select.From, String],
              val exprNaming: Map[Expr.Identity, SqlStr],
              val tableNameMapper: String => String,
              val columnNameMapper: String => String){
//  assert(!fromNaming.toString().contains("purchase"))
}
