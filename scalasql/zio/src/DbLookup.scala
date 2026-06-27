package scalasql

sealed abstract class DbLookupException[T](message: String)(using val entityName: DbEntityName[T])
    extends Exception(
      message
    )

object DbLookupException:
  final class MultipleResults[T: DbEntityName as name]
      extends DbLookupException(s"Multiple entries of $name exist")

  final class NotFound[T: DbEntityName as name]
      extends DbLookupException(s"Expected $name does not exist")

/** Lightweight typeclass that provides entity name information */
object DbEntityName:
  opaque type Of[T] <: String = String

  inline def apply[T](name: String): Of[T] = name

  inline given scalaTypeName[T]: Of[T] = ${ typeReprImpl[T] }

  import scala.quoted.Expr
  import scala.quoted.Type
  import scala.quoted.Quotes

  /* Unlike a `ClassTag` works with opaque types and doesn't drop type parameters */
  def typeReprImpl[T: Type](using q: Quotes): Expr[String] = Expr(
    q.reflect.TypeRepr
      .of[T]
      .dealias
      .simplified
      .show
      .split('.')
      .filterNot(_.endsWith("$package"))
      .mkString(".")
  )

type DbEntityName[T] = DbEntityName.Of[T]
