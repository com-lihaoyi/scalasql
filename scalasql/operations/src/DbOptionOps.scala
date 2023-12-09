package scalasql.operations

import scalasql.core.TypeMapper
import scalasql.core.DialectTypeMappers
import scalasql.core.Db
import scalasql.core.Context
import scalasql.core.SqlStr.SqlStringSyntax

class DbOptionOps[T: TypeMapper](v: Db[Option[T]])(implicit dialect: DialectTypeMappers) {
  import dialect._

  def isDefined: Db[Boolean] = Db { implicit ctx => sql"($v IS NOT NULL)" }

  def isEmpty: Db[Boolean] = Db { implicit ctx => sql"($v IS NULL)" }

  // SQL nulls tend to propagate automatically, so we do not need to explicitly
  // generate CASE/WHEN/THEN/ELSE syntax and can just use the final expression directly
  // and assume the nulls will propagate as necessary
  def map[V: TypeMapper](f: Db[T] => Db[V]): Db[Option[V]] = Db { implicit ctx =>
    sql"${f(v.asInstanceOf[Db[T]])}"
  }

  def flatMap[V: TypeMapper](f: Db[T] => Db[Option[V]]): Db[Option[V]] =
    Db { implicit ctx => sql"${f(v.asInstanceOf[Db[T]])}" }

  def get: Db[T] = Db[T] { implicit ctx: Context => sql"$v" }

  def getOrElse(other: Db[T]): Db[T] = Db[T] { implicit ctx: Context =>
    sql"COALESCE($v, $other)"
  }

  def orElse(other: Db[Option[T]]): Db[Option[T]] = Db[T] { implicit ctx: Context =>
    sql"COALESCE($v, $other)"
  }

  def filter(other: Db[T] => Db[Boolean]): Db[Option[T]] = new CaseWhen.Else[Option[T]](
    Seq(other(Db[T] { implicit ctx: Context => sql"$v" }) -> v),
    Db { implicit ctx => sql"NULL" }
  )
}
