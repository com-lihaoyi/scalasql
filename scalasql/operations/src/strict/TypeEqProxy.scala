package scalasql.operations.strict

import scalasql.core.Expr
import scala.util.NotGiven

trait TypeEqProxy[A, B]

object TypeEqProxy {
  private[strict] lazy val typeEq: TypeEqProxy[Unit, Unit] = new TypeEqProxy {}

  given [A, B](using NotGiven[StrictMode]): TypeEqProxy[A, B] =
    typeEq.asInstanceOf[TypeEqProxy[A, B]]
}
