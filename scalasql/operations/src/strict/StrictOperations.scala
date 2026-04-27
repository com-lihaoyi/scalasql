package scalasql.operations.strict

trait StrictOperations {
  given StrictMode = new StrictMode {}
  given [A, B](using A =:= B): TypeEqProxy[A, B] =
    TypeEqProxy.typeEq.asInstanceOf[TypeEqProxy[A, B]]
  given [A, B](using A =:= B): TypeEqProxy[Option[A], B] =
    TypeEqProxy.typeEq.asInstanceOf[TypeEqProxy[Option[A], B]]
  given [A, B](using A =:= B): TypeEqProxy[A, Option[B]] =
    TypeEqProxy.typeEq.asInstanceOf[TypeEqProxy[A, Option[B]]]
}

object StrictOperations extends StrictOperations
