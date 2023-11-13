package scalasql.operations

import scalasql.query.Expr
import scalasql.renderer.SqlStr.SqlStringSyntax

import scala.reflect.ClassTag

class ExprTypedOps[T: ClassTag](v: Expr[T]) {

  private def isNullable[T: ClassTag] = implicitly[ClassTag[T]].runtimeClass == classOf[Option[_]]

  /** Equals to */
  def ===[V: ClassTag](x: Expr[V]): Expr[Boolean] = Expr { implicit ctx =>
    (isNullable[T], isNullable[V]) match {
      case (true, true) => sql"($v IS NULL AND $x IS NULL) OR $v = $x"
      case _ => sql"$v = $x"
    }
  }

  /** Not equal to */
  def !==[V: ClassTag](x: Expr[V]): Expr[Boolean] = Expr { implicit ctx =>
    (isNullable[T], isNullable[V]) match {
      case (true, true) => sql"($v IS NULL AND $x IS NULL) OR $v = $x"
      case _ => sql"$v <> $x"
    }
  }

}
