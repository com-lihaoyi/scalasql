package scalasql.operations

import scalasql.core.Expr

/** TypeEq indicates that values of two given types can be compared with each other. */
trait TypeEq[A, B]

private lazy val typeEq: TypeEq[Unit, Unit] = new TypeEq {}

object TypeEq extends LowLevelDeprecated {
  given [A, B](using A =:= B): TypeEq[A, B] =
    typeEq.asInstanceOf[TypeEq[A, B]]
}

trait LowLevelDeprecated {
  @deprecated(
    "Comparison operators for optional types is deprecated due broken typesafety - you may get unexpected results due to null propagation. Consider .map, .filter combined with .isDefined / isEmpty methods available on Expr[Option[A]]"
  )
  given [A, B](using A =:= B): TypeEq[Option[A], B] =
    typeEq.asInstanceOf[TypeEq[Option[A], B]]

  @deprecated(
    "Comparison operators for optional types is deprecated due broken typesafety - you may get unexpected results due to null propagation. Consider .map, .filter combined with .isDefined / isEmpty methods available on Expr[Option[A]]"
  )
  given [A, B](using A =:= B): TypeEq[A, Option[B]] =
    typeEq.asInstanceOf[TypeEq[A, Option[B]]]

  @deprecated(
    "Unsafe comparison is deprecated and soon will be removed, import scalasql.operations.unsafeEq.given to surpress warnings"
  )
  given [A, B]: TypeEq[A, B] =
    typeEq.asInstanceOf[TypeEq[A, B]]
}

trait unsafeEq {
  given [A, B]: TypeEq[A, B] = typeEq.asInstanceOf[TypeEq[A, B]]
  given [A, B](using A =:= B): TypeEq[A, Option[B]] =
    typeEq.asInstanceOf[TypeEq[A, Option[B]]]
  given [A, B](using A =:= B): TypeEq[Option[A], B] =
    typeEq.asInstanceOf[TypeEq[Option[A], B]]
}
object unsafeEq extends unsafeEq
