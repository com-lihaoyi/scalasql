package scalasql.operations

import scalasql.TypeMapper
import scalasql.query.{Expr, JoinNullable}
import scalasql.renderer.SqlStr
import scalasql.renderer.SqlStr.SqlStringSyntax

import scala.reflect.ClassTag

class ExprTypedOps[T: ClassTag](v: Expr[T]) {

  private def isNullable[T: ClassTag](value: Expr[T]) =
    implicitly[ClassTag[T]].runtimeClass == classOf[Option[_]] ||
      implicitly[ClassTag[T]].runtimeClass == classOf[JoinNullable[_]]

  /** Equals to */
  def ===[V: ClassTag](x: Expr[V]): Expr[Boolean] = Expr { implicit ctx =>
    (isNullable(v), isNullable(x)) match {
      case (true, true) => sql"($v IS NULL AND $x IS NULL) OR $v = $x"
      case _ => sql"$v = $x"
    }
  }

  /** Not equal to */
  def !==[V: ClassTag](x: Expr[V]): Expr[Boolean] = Expr { implicit ctx =>
    (isNullable(v), isNullable(x)) match {
      case (true, true) => sql"($v IS NULL AND $x IS NULL) OR $v = $x"
      case (false, false) => sql"$v <> $x"
    }
  }

}
