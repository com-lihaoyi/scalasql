package scalasql.operations

import scalasql.MappedType
import scalasql.query.Expr
import scalasql.renderer.SqlStr.SqlStringSyntax

class ExprOptionOps[T](v: Expr[Option[T]]) {

  def isDefined: Expr[Boolean] = Expr { implicit ctx => sql"$v IS NOT NULL" }

  def isEmpty: Expr[Boolean] = Expr { implicit ctx => sql"$v IS NULL" }

  // SQL nulls tend to propagate automatically, so we do not need to explicitly
  // generate CASE/WHEN/THEN/ELSE syntax and can just use the final expression directly
  // and assume the nulls will propagate as necessary
  def map[V: MappedType](f: Expr[T] => Expr[V]): Expr[Option[V]] = Expr { implicit ctx =>
    sql"${f(v.asInstanceOf[Expr[T]])}"
  }

  def flatMap[V: MappedType](f: Expr[T] => Expr[Option[V]]): Expr[Option[V]] =
    Expr { implicit ctx => sql"${f(v.asInstanceOf[Expr[T]])}" }
}
