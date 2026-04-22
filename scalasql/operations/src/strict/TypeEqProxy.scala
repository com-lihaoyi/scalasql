package scalasql.operations.strict

import scalasql.core.Expr
import scala.util.NotGiven

trait TypeEqProxy[A, B]

object TypeEqProxy {
  given notStrictEqGiven[A, B](using NotGiven[StrictMode]): TypeEqProxy[A, B] = new TypeEqProxy {}
}
