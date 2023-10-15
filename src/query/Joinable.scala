package usql.query

trait Joinable[T] {
  def select: Select[T]
}
