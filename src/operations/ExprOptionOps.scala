package scalasql.operations

import scalasql.MappedType
import scalasql.query.Expr
import scalasql.renderer.SqlStr.SqlStringSyntax

class ExprOptionOps[T](v: Expr[Option[T]]) {

  def isDefined: Expr[Boolean] = Expr { implicit ctx => sql"$v IS NOT NULL" }

  def isEmpty: Expr[Boolean] = Expr { implicit ctx => sql"$v IS NULL" }

  def map[V: MappedType](f: Expr[T] => Expr[V]): Expr[Option[V]] = Expr{ implicit ctx =>
    sql"CASE WHEN $v IS NOT NULL THEN ${f(v.asInstanceOf[Expr[T]])} ELSE NULL"
  }
}
