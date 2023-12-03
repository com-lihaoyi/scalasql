package scalasql.operations

import scalasql.core.TypeMapper
import scalasql.core.Sql
import scalasql.core.SqlStr
import scalasql.core.SqlStr.SqlStringSyntax

class SqlOps(v: Sql[_]) {

  /**
   * SQL-style Equals to, translates to SQL `=`. Returns `false` if both values are `NULL`
   */
  def `=`[T](x: Sql[T]): Sql[Boolean] = Sql { implicit ctx => sql"($v = $x)" }

  /**
   * SQL-style Not equals to, translates to SQL `<>`. Returns `false` if both values are `NULL`
   */
  def <>[T](x: Sql[T]): Sql[Boolean] = Sql { implicit ctx => sql"($v <> $x)" }

  /** Greater than */
  def >[V](x: Sql[V]): Sql[Boolean] = Sql { implicit ctx => sql"($v > $x)" }

  /** Less than */
  def <[V](x: Sql[V]): Sql[Boolean] = Sql { implicit ctx => sql"($v < $x)" }

  /** Greater than or equal to */
  def >=[V](x: Sql[V]): Sql[Boolean] = Sql { implicit ctx => sql"($v >= $x)" }

  /** Less than or equal to */
  def <=[V](x: Sql[V]): Sql[Boolean] = Sql { implicit ctx => sql"($v <= $x)" }

  /** Translates to a SQL `CAST` from one type to another */
  def cast[V: TypeMapper]: Sql[V] = Sql { implicit ctx =>
    sql"CAST($v AS ${SqlStr.raw(implicitly[TypeMapper[V]].castTypeString)})"
  }

  /**
   * Similar to [[cast]], but allows you to pass in an explicit [[SqlStr]] to
   * further specify the SQL type you want to cast to
   */
  def castNamed[V: TypeMapper](typeName: SqlStr): Sql[V] = Sql { implicit ctx =>
    sql"CAST($v AS $typeName)"
  }
}
