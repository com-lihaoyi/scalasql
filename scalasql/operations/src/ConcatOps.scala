package scalasql.operations
import scalasql.core.{Expr, SqlStr}
import scalasql.core.SqlStr.SqlStringSyntax

trait ConcatOps {

  /**
   * Concatenate all arguments. NULL arguments are ignored.
   */
  def concat(values: Expr[?]*): Expr[String] = Expr { implicit ctx =>
    sql"CONCAT(${SqlStr.join(values.map(v => sql"$v"), SqlStr.commaSep)})"
  }

  /**
   * Concatenate all but first arguments with separators. The first parameter is used
   * as a separator. NULL arguments are ignored.
   */
  def concatWs(sep: Expr[String], values: Expr[?]*): Expr[String] = Expr { implicit ctx =>
    sql"CONCAT_WS($sep, ${SqlStr.join(values.map(v => sql"$v"), SqlStr.commaSep)})"
  }
}
