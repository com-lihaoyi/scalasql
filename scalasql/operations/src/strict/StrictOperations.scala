package scalasql.operations.strict

trait StrictOperations {
  given StrictMode = new StrictMode {}
  given [A, B](using A =:= B): TypeEqProxy[A, B] = new TypeEqProxy {}
  given [A, B](using A =:= B): TypeEqProxy[Option[A], B] = new TypeEqProxy {}
  given [A, B](using A =:= B): TypeEqProxy[A, Option[B]] = new TypeEqProxy {}
}

object StrictOperations extends StrictOperations
