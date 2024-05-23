package scalasql.operations

import scalasql.core.TypeMapper
import scalasql.core.Expr
import scalasql.core.SqlStr
import scalasql.core.SqlStr.SqlStringSyntax

class ExprOps(v: Expr[?]) {

  /**
   * SQL-style Equals to, translates to SQL `=`. Returns `false` if both values are `NULL`
   */
  def `=`[T](x: Expr[T]): Expr[Boolean] = Expr { implicit ctx => sql"($v = $x)" }

  /**
   * SQL-style Not equals to, translates to SQL `<>`. Returns `false` if both values are `NULL`
   */
  def <>[T](x: Expr[T]): Expr[Boolean] = Expr { implicit ctx => sql"($v <> $x)" }

  /** Greater than */
  def >[V](x: Expr[V]): Expr[Boolean] = Expr { implicit ctx => sql"($v > $x)" }

  /** Less than */
  def <[V](x: Expr[V]): Expr[Boolean] = Expr { implicit ctx => sql"($v < $x)" }

  /** Greater than or equal to */
  def >=[V](x: Expr[V]): Expr[Boolean] = Expr { implicit ctx => sql"($v >= $x)" }

  /** Less than or equal to */
  def <=[V](x: Expr[V]): Expr[Boolean] = Expr { implicit ctx => sql"($v <= $x)" }

  /** Translates to a SQL `CAST` from one type to another */
  def cast[V: TypeMapper]: Expr[V] = Expr { implicit ctx =>
    sql"CAST($v AS ${SqlStr.raw(implicitly[TypeMapper[V]].castTypeString)})"
  }

  /**
   * Similar to [[cast]], but allows you to pass in an explicit [[SqlStr]] to
   * further specify the SQL type you want to cast to
   */
  def castNamed[V: TypeMapper](typeName: SqlStr): Expr[V] = Expr { implicit ctx =>
    sql"CAST($v AS $typeName)"
  }
}
