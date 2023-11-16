package object scalasql {
  type Id[T] = T

  val Expr = query.Expr
  type Expr[T] = query.Expr[T]

  val Values = query.Values
  type Values[T] = query.Values[T]
}
