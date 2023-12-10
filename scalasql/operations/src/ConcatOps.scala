package scalasql.operations
import scalasql.core.{Db, SqlStr}
import scalasql.core.SqlStr.SqlStringSyntax

trait ConcatOps {

  /**
   * Concatenate all arguments. NULL arguments are ignored.
   */
  def concat(values: Db[_]*): Db[String] = Db { implicit ctx =>
    sql"CONCAT(${SqlStr.join(values.map(v => sql"$v"), SqlStr.commaSep)})"
  }

  /**
   * Concatenate all but first arguments with separators. The first parameter is used
   * as a separator. NULL arguments are ignored.
   */
  def concatWs(sep: Db[String], values: Db[_]*): Db[String] = Db { implicit ctx =>
    sql"CONCAT_WS($sep, ${SqlStr.join(values.map(v => sql"$v"), SqlStr.commaSep)})"
  }
}
