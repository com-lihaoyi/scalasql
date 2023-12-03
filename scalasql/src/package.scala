package object scalasql {
  type Id[T] = T

  val Sql = query.Sql
  type Sql[T] = query.Sql[T]

}
