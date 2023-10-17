package usql

import renderer.{Context, SelectToSql, SqlStr}
import usql.query.Expr

/**
 * Converts back and forth between a tree-shaped JSON and flat key-value map
 */
object FlatJson {

  val delimiter = "__"
  val basePrefix = "res"

  def flatten(x: Seq[(List[String], Expr[_])],
              context: Context): Seq[(String, SqlStr)] = {
    x.map{case (k, v) => ((basePrefix +: k).mkString(delimiter), v.toSqlExpr(context))}
  }
}
