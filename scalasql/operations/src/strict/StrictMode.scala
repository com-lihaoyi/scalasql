package scalasql.operations.strict

trait StrictMode

given StrictMode = new StrictMode {}
given [A, B](using A =:= B): TypeEqProxy[A, B] = new TypeEqProxy {}
given [A, B](using A =:= B): TypeEqProxy[Option[A], B] = new TypeEqProxy {}
given [A, B](using A =:= B): TypeEqProxy[A, Option[B]] = new TypeEqProxy {}
