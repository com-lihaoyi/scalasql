package scalasql.operations.strict

import scalasql.core.Expr

// TODO: after upgrading to a newer scala version this trait could become `extends compiletime.Erased`
/** TypeEq indicates that values of two given types can be compared with each other. */
trait TypeEq[A, B]

private lazy val typeEq: TypeEq[Unit, Unit] = new TypeEq {}

object TypeEq {
  // given StrictMode = new StrictMode {}
  given [A, B](using A =:= B): TypeEq[A, B] =
    typeEq.asInstanceOf[TypeEq[A, B]]
  given [A, B](using A =:= B): TypeEq[Option[A], B] =
    typeEq.asInstanceOf[TypeEq[Option[A], B]]
  given [A, B](using A =:= B): TypeEq[A, Option[B]] =
    typeEq.asInstanceOf[TypeEq[A, Option[B]]]

  @deprecated(
    "Unsafe comparison is deprecated and soon will be removed, import unsafeEq.given to surpress warnings"
  )
  given [A, B]: TypeEq[A, B] =
    typeEq.asInstanceOf[TypeEq[A, B]]
}

trait unsafeEq {
  given [A, B]: TypeEq[A, B] = typeEq.asInstanceOf[TypeEq[A, B]]
}
object unsafeEq extends unsafeEq
