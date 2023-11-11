package scalasql.operations

import scalasql.MappedType
import scalasql.query.Expr
import scalasql.renderer.Context
import scalasql.renderer.SqlStr.SqlStringSyntax

class ExprOptionOps[T: MappedType](v: Expr[Option[T]]) {

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

  def get: Expr[T] = Expr[T] { implicit ctx: Context => sql"$v" }

  def getOrElse(other: Expr[T]): Expr[T] = Expr[T] { implicit ctx: Context =>
    sql"COALESCE($v, $other)"
  }

  def orElse(other: Expr[Option[T]]): Expr[Option[T]] = Expr[T] { implicit ctx: Context =>
    sql"COALESCE($v, $other)"
  }

  def filter(other: Expr[T] => Expr[Boolean]): Expr[Option[T]] = new CaseWhen.Else[Option[T]](
    Seq(other(Expr[T] { implicit ctx: Context => sql"$v" }) -> v),
    Expr { implicit ctx => sql"NULL" }
  )
}
