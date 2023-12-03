package scalasql.operations

import scalasql.core.TypeMapper
import scalasql.dialects.Dialect
import scalasql.core.Sql
import scalasql.renderer.Context
import scalasql.core.SqlStr.SqlStringSyntax

class ExprOptionOps[T: TypeMapper](v: Sql[Option[T]])(implicit dialect: Dialect) {
  import dialect._

  def isDefined: Sql[Boolean] = Sql { implicit ctx => sql"($v IS NOT NULL)" }

  def isEmpty: Sql[Boolean] = Sql { implicit ctx => sql"($v IS NULL)" }

  // SQL nulls tend to propagate automatically, so we do not need to explicitly
  // generate CASE/WHEN/THEN/ELSE syntax and can just use the final expression directly
  // and assume the nulls will propagate as necessary
  def map[V: TypeMapper](f: Sql[T] => Sql[V]): Sql[Option[V]] = Sql { implicit ctx =>
    sql"${f(v.asInstanceOf[Sql[T]])}"
  }

  def flatMap[V: TypeMapper](f: Sql[T] => Sql[Option[V]]): Sql[Option[V]] =
    Sql { implicit ctx => sql"${f(v.asInstanceOf[Sql[T]])}" }

  def get: Sql[T] = Sql[T] { implicit ctx: Context => sql"$v" }

  def getOrElse(other: Sql[T]): Sql[T] = Sql[T] { implicit ctx: Context =>
    sql"COALESCE($v, $other)"
  }

  def orElse(other: Sql[Option[T]]): Sql[Option[T]] = Sql[T] { implicit ctx: Context =>
    sql"COALESCE($v, $other)"
  }

  def filter(other: Sql[T] => Sql[Boolean]): Sql[Option[T]] = new CaseWhen.Else[Option[T]](
    Seq(other(Sql[T] { implicit ctx: Context => sql"$v" }) -> v),
    Sql { implicit ctx => sql"NULL" }
  )
}
