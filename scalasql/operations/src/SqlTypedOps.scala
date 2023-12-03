package scalasql.operations

import scalasql.core.Sql
import scalasql.core.SqlStr.SqlStringSyntax

import scala.reflect.ClassTag

class SqlTypedOps[T: ClassTag](v: Sql[T]) {

  protected def isNullable[T: ClassTag] = implicitly[ClassTag[T]].runtimeClass == classOf[Option[_]]

  /**
   * Scala-style Equals to, returns `true` if both values are `NULL`.
   * Translates to `IS NOT DISTINCT FROM` if both values are nullable,
   * otherwise translates to `=`
   */
  def ===[V: ClassTag](x: Sql[V]): Sql[Boolean] = Sql { implicit ctx =>
    (isNullable[T], isNullable[V]) match {
      case (true, true) => sql"($v IS NOT DISTINCT FROM $x)"
      case _ => sql"($v = $x)"
    }
  }

  /**
   * Scala-style Not equals to, returns `false` if both values are `NULL`
   * Translates to `IS DISTINCT FROM` if both values are nullable,
   * otherwise translates to `<>`
   */
  def !==[V: ClassTag](x: Sql[V]): Sql[Boolean] = Sql { implicit ctx =>
    (isNullable[T], isNullable[V]) match {
      case (true, true) => sql"($v IS DISTINCT FROM $x)"
      case _ => sql"($v <> $x)"
    }
  }
}
